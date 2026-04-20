package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.ClassRoom;
import com.laby.annual.scheduler.entity.Subject;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.ClassRoomRepository;
import com.laby.annual.scheduler.repository.SubjectRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.TutorSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnnualTimetableImportService {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "schoolId",
            "classRoomId",
            "dayOfWeek",
            "periodNumber",
            "tutorId",
            "subjectId",
            "subjectName"
    );

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;
    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final TutorRepository tutorRepository;
    private final TutorSubjectRepository tutorSubjectRepository;

    @Transactional
    public Map<String, Object> importAnnualTimetable(
            MultipartFile file,
            LocalDate academicYearStart,
            LocalDate academicYearEnd,
            Long schoolIdFromToken
    ) {
        validateDateRange(academicYearStart, academicYearEnd);

        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        boolean isCsv = filename.endsWith(".csv");
        boolean isXlsx = filename.endsWith(".xlsx");

        if (!isCsv && !isXlsx) {
            throw new RuntimeException("Unsupported file type. Please upload CSV or XLSX");
        }

        List<UploadRow> rows = isCsv
                ? parseCsv(file)
                : parseXlsx(file);

        Map<String, String> errors = new LinkedHashMap<>();
        List<AnnualTimetableEntry> entries = new ArrayList<>();
        Map<String, AnnualTimetableEntry> slotByClass = new HashMap<>();
        Map<String, Integer> tutorDayLoad = new HashMap<>();
        Map<String, Boolean> tutorSlotUsed = new HashMap<>();

        for (UploadRow row : rows) {
            String rowKey = "Row " + row.rowNumber;
            if (row.schoolId == null || !Objects.equals(row.schoolId, schoolIdFromToken)) {
                errors.put(rowKey, "schoolId mismatch with token");
                continue;
            }

            if (row.classRoomId == null) {
                errors.put(rowKey, "classRoomId is required");
                continue;
            }

            Optional<ClassRoom> classRoomOpt = classRoomRepository.findById(row.classRoomId);
            if (classRoomOpt.isEmpty() || !Objects.equals(classRoomOpt.get().getSchoolId(), schoolIdFromToken)) {
                errors.put(rowKey, "classRoomId not found for school");
                continue;
            }

            if (row.dayOfWeek == null || row.periodNumber <= 0) {
                errors.put(rowKey, "dayOfWeek and periodNumber are required");
                continue;
            }

            Subject subject = resolveSubject(row, classRoomOpt.get(), schoolIdFromToken);
            if (subject == null) {
                errors.put(rowKey, "subjectId or subjectName not found for class grade");
                continue;
            }

            String tutorId = null;
            if (row.tutorId != null && !row.tutorId.isBlank()) {
                try {
                    tutorId = resolveTutorId(row.tutorId, schoolIdFromToken, row.rowNumber);
                } catch (RuntimeException ex) {
                    errors.put(rowKey, ex.getMessage());
                    continue;
                }
            } else {
                tutorId = autoAssignTutor(subject, row.dayOfWeek, row.periodNumber, schoolIdFromToken,
                        tutorDayLoad, tutorSlotUsed);
                if (tutorId == null) {
                    AnnualTimetableEntry conflict = buildEntry(
                            row,
                            subject,
                            schoolIdFromToken,
                            null,
                            academicYearStart,
                            academicYearEnd,
                            AnnualTimetableEntry.Status.CONFLICT,
                            AnnualTimetableEntry.ConflictReason.NO_TUTOR_AVAILABLE
                    );
                    entries.add(conflict);
                    errors.put(rowKey, "tutorId is empty and no available tutor for subject");
                    continue;
                }
            }

            String classSlotKey = row.classRoomId + "|" + row.dayOfWeek + "|" + row.periodNumber;
            if (slotByClass.containsKey(classSlotKey)) {
                AnnualTimetableEntry existing = slotByClass.get(classSlotKey);
                existing.setStatus(AnnualTimetableEntry.Status.CONFLICT);
                existing.setConflictReason(AnnualTimetableEntry.ConflictReason.NO_TUTOR_AVAILABLE);
                errors.put(rowKey, "duplicate class slot in file");
                continue;
            }

            AnnualTimetableEntry entry = buildEntry(
                    row,
                    subject,
                    schoolIdFromToken,
                    tutorId,
                    academicYearStart,
                    academicYearEnd,
                    AnnualTimetableEntry.Status.ASSIGNED,
                    null
            );

            slotByClass.put(classSlotKey, entry);
            entries.add(entry);
            trackTutorLoad(tutorId, row.dayOfWeek, row.periodNumber, tutorDayLoad, tutorSlotUsed);
        }

        markConflicts(entries, schoolIdFromToken, academicYearStart, academicYearEnd);

        annualTimetableEntryRepository.deleteBySchoolIdAndAcademicYearStartAndAcademicYearEnd(
                schoolIdFromToken,
                academicYearStart,
                academicYearEnd
        );
        annualTimetableEntryRepository.saveAll(entries);

        long conflictCount = entries.stream()
                .filter(e -> e.getStatus() == AnnualTimetableEntry.Status.CONFLICT)
                .count();
        List<Map<String, Object>> conflictEntries = entries.stream()
                .filter(e -> e.getStatus() == AnnualTimetableEntry.Status.CONFLICT)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("classRoomId", e.getClassRoomId());
                    item.put("dayOfWeek", e.getDayOfWeek());
                    item.put("periodNumber", e.getPeriodNumber());
                    item.put("subjectId", e.getSubjectId());
                    item.put("subjectName", e.getSubjectName());
                    item.put("tutorId", e.getTutorId());
                    item.put("reason", e.getConflictReason());
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schoolId", schoolIdFromToken);
        response.put("academicYearStart", academicYearStart);
        response.put("academicYearEnd", academicYearEnd);
        response.put("rowsRead", rows.size());
        response.put("slotsInserted", entries.size());
        response.put("conflicts", conflictCount);
        response.put("conflictEntries", conflictEntries);
        response.put("errors", errors);
        return response;
    }

    private void markConflicts(
            List<AnnualTimetableEntry> entries,
            Long schoolId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    ) {
        Map<String, List<AnnualTimetableEntry>> byTutorSlot = new HashMap<>();
        for (AnnualTimetableEntry entry : entries) {
            if (entry.getTutorId() == null || entry.getDayOfWeek() == null) {
                continue;
            }
            String key = entry.getTutorId() + "|" + entry.getDayOfWeek() + "|" + entry.getPeriodNumber();
            byTutorSlot.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        for (List<AnnualTimetableEntry> slotEntries : byTutorSlot.values()) {
            if (slotEntries.size() > 1) {
                for (AnnualTimetableEntry entry : slotEntries) {
                    entry.setStatus(AnnualTimetableEntry.Status.CONFLICT);
                    entry.setConflictReason(AnnualTimetableEntry.ConflictReason.NO_TUTOR_AVAILABLE);
                }
            }
        }

        Map<String, List<AnnualTimetableEntry>> byTutorDay = new HashMap<>();
        for (AnnualTimetableEntry entry : entries) {
            if (entry.getTutorId() == null || entry.getDayOfWeek() == null) {
                continue;
            }
            String key = entry.getTutorId() + "|" + entry.getDayOfWeek();
            byTutorDay.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<AnnualTimetableEntry>> entry : byTutorDay.entrySet()) {
            String tutorId = entry.getKey().split("\\|")[0];
            int maxPerDay = tutorRepository.findById(tutorId)
                    .map(Tutor::getMaxClassesPerDay)
                    .orElse(5);
            List<AnnualTimetableEntry> dayEntries = entry.getValue();
            dayEntries.sort(Comparator.comparingInt(AnnualTimetableEntry::getPeriodNumber)
                    .thenComparingLong(e -> e.getClassRoomId() == null ? 0L : e.getClassRoomId()));
            for (int i = maxPerDay; i < dayEntries.size(); i++) {
                AnnualTimetableEntry conflict = dayEntries.get(i);
                conflict.setStatus(AnnualTimetableEntry.Status.CONFLICT);
                conflict.setConflictReason(AnnualTimetableEntry.ConflictReason.NO_TUTOR_AVAILABLE);
            }
        }
    }

    private List<UploadRow> parseCsv(MultipartFile file) {
        List<UploadRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV header not found");
            }

            String[] headers = headerLine.split(",");
            validateHeaders(headers);

            String line;
            int rowIndex = 1;
            while ((line = reader.readLine()) != null) {
                rowIndex++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                UploadRow row = parseRow(values, rowIndex);
                rows.add(row);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }
        return rows;
    }

    private List<UploadRow> parseXlsx(MultipartFile file) {
        List<UploadRow> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel header not found");
            }
            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headers.length; i++) {
                headers[i] = DATA_FORMATTER.formatCellValue(headerRow.getCell(i)).trim();
            }
            validateHeaders(headers);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String[] values = new String[REQUIRED_HEADERS.size()];
                boolean hasValue = false;
                for (int c = 0; c < values.length; c++) {
                    values[c] = DATA_FORMATTER.formatCellValue(row.getCell(c)).trim();
                    if (!values[c].isBlank()) {
                        hasValue = true;
                    }
                }
                if (!hasValue) {
                    continue;
                }
                rows.add(parseRow(values, i + 1));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file", e);
        }
        return rows;
    }

    private UploadRow parseRow(String[] values, int rowNumber) {
        UploadRow row = new UploadRow();
        row.rowNumber = rowNumber;
        row.schoolId = parseLong(values[0]);
        row.classRoomId = parseLong(values[1]);
        row.dayOfWeek = parseDay(values[2]);
        row.periodNumber = parseInt(values[3]);
        row.tutorId = normalize(values[4]);
        row.subjectId = parseLong(values[5]);
        row.subjectName = normalize(values[6]);
        return row;
    }

    private void validateHeaders(String[] headers) {
        if (headers.length < REQUIRED_HEADERS.size()) {
            throw new RuntimeException("Invalid header. Expected columns: " + REQUIRED_HEADERS);
        }
        for (int i = 0; i < REQUIRED_HEADERS.size(); i++) {
            String actual = headers[i] == null ? "" : headers[i].trim();
            String expected = REQUIRED_HEADERS.get(i);
            if (!expected.equalsIgnoreCase(actual)) {
                throw new RuntimeException("Invalid header at column " + (i + 1)
                        + ". expected '" + expected + "' but found '" + actual + "'");
            }
        }
    }

    private DayOfWeek parseDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "MON", "MONDAY" -> {
                return DayOfWeek.MONDAY;
            }
            case "TUE", "TUESDAY" -> {
                return DayOfWeek.TUESDAY;
            }
            case "WED", "WEDNESDAY" -> {
                return DayOfWeek.WEDNESDAY;
            }
            case "THU", "THURSDAY" -> {
                return DayOfWeek.THURSDAY;
            }
            case "FRI", "FRIDAY" -> {
                return DayOfWeek.FRIDAY;
            }
            case "SAT", "SATURDAY" -> {
                return DayOfWeek.SATURDAY;
            }
            default -> throw new RuntimeException("Invalid dayOfWeek: " + value);
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            try {
                double parsed = Double.parseDouble(normalized);
                return (long) parsed;
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("P")) {
            normalized = normalized.substring(1);
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            try {
                double parsed = Double.parseDouble(normalized);
                return (int) parsed;
            } catch (NumberFormatException ignore) {
                return 0;
            }
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Subject resolveSubject(UploadRow row, ClassRoom classRoom, Long schoolId) {
        if (row.subjectId != null) {
            Optional<Subject> subject = subjectRepository.findById(row.subjectId);
            if (subject.isPresent()
                    && Objects.equals(subject.get().getSchoolId(), schoolId)
                    && Objects.equals(subject.get().getGrade(), classRoom.getGrade())) {
                return subject.get();
            }
        }
        if (row.subjectName == null || row.subjectName.isBlank()) {
            return null;
        }

        List<Subject> candidates = subjectRepository.findBySchoolIdAndGradeAndActiveTrue(
                schoolId,
                classRoom.getGrade()
        );
        for (Subject subject : candidates) {
            if (subject.getName() != null
                    && normalizeSubjectName(subject.getName()).equals(normalizeSubjectName(row.subjectName))) {
                return subject;
            }
        }
        return null;
    }

    private String resolveTutorId(String tutorValue, Long schoolId, int rowIndex) {
        String normalized = tutorValue == null ? "" : tutorValue.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException("Row " + rowIndex + ": tutorId is empty");
        }

        Optional<Tutor> byId = tutorRepository.findByTutorId(normalized);
        if (byId.isPresent() && Objects.equals(byId.get().getSchoolId(), schoolId) && byId.get().isActive()) {
            return byId.get().getTutorId();
        }

        Optional<Tutor> byCode = tutorRepository.findByTutorCode(normalized);
        if (byCode.isPresent() && Objects.equals(byCode.get().getSchoolId(), schoolId) && byCode.get().isActive()) {
            return byCode.get().getTutorId();
        }

        throw new RuntimeException("Row " + rowIndex + ": tutor not found or not active for school -> " + normalized);
    }

    private void validateDateRange(LocalDate academicYearStart, LocalDate academicYearEnd) {
        if (academicYearStart == null || academicYearEnd == null) {
            throw new RuntimeException("academicYearStart and academicYearEnd are required");
        }
        if (academicYearStart.isAfter(academicYearEnd)) {
            throw new RuntimeException("academicYearStart cannot be after academicYearEnd");
        }
    }

    private AnnualTimetableEntry buildEntry(
            UploadRow row,
            Subject subject,
            Long schoolId,
            String tutorId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd,
            AnnualTimetableEntry.Status status,
            AnnualTimetableEntry.ConflictReason reason
    ) {
        AnnualTimetableEntry entry = new AnnualTimetableEntry();
        entry.setSchoolId(schoolId);
        entry.setClassRoomId(row.classRoomId);
        entry.setSubjectId(subject.getId());
        entry.setSubjectName(subject.getName());
        entry.setTutorId(tutorId);
        entry.setDayOfWeek(row.dayOfWeek);
        entry.setPeriodNumber(row.periodNumber);
        entry.setAcademicYearStart(academicYearStart);
        entry.setAcademicYearEnd(academicYearEnd);
        entry.setStatus(status);
        entry.setConflictReason(reason);
        entry.setActive(true);
        return entry;
    }

    private String autoAssignTutor(
            Subject subject,
            DayOfWeek dayOfWeek,
            int periodNumber,
            Long schoolId,
            Map<String, Integer> tutorDayLoad,
            Map<String, Boolean> tutorSlotUsed
    ) {
        if (subject == null || dayOfWeek == null || periodNumber <= 0) {
            return null;
        }

        List<String> tutorIds = tutorSubjectRepository.findTutorIdsBySubjectId(subject.getId());
        if (tutorIds.isEmpty()) {
            return null;
        }

        String bestTutor = null;
        int bestLoad = Integer.MAX_VALUE;
        for (String tutorId : tutorIds) {
            Optional<Tutor> tutorOpt = tutorRepository.findById(tutorId);
            if (tutorOpt.isEmpty()) {
                continue;
            }
            Tutor tutor = tutorOpt.get();
            if (!Objects.equals(tutor.getSchoolId(), schoolId) || !tutor.isActive()) {
                continue;
            }

            String slotKey = tutorId + "|" + dayOfWeek + "|" + periodNumber;
            if (Boolean.TRUE.equals(tutorSlotUsed.get(slotKey))) {
                continue;
            }

            int maxPerDay = tutor.getMaxClassesPerDay() > 0 ? tutor.getMaxClassesPerDay() : 5;
            String dayKey = tutorId + "|" + dayOfWeek;
            int load = tutorDayLoad.getOrDefault(dayKey, 0);
            if (load >= maxPerDay) {
                continue;
            }

            if (load < bestLoad) {
                bestLoad = load;
                bestTutor = tutorId;
            }
        }

        return bestTutor;
    }

    private void trackTutorLoad(
            String tutorId,
            DayOfWeek dayOfWeek,
            int periodNumber,
            Map<String, Integer> tutorDayLoad,
            Map<String, Boolean> tutorSlotUsed
    ) {
        if (tutorId == null || dayOfWeek == null || periodNumber <= 0) {
            return;
        }
        String dayKey = tutorId + "|" + dayOfWeek;
        tutorDayLoad.put(dayKey, tutorDayLoad.getOrDefault(dayKey, 0) + 1);
        String slotKey = tutorId + "|" + dayOfWeek + "|" + periodNumber;
        tutorSlotUsed.put(slotKey, true);
    }

    private String normalizeSubjectName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static class UploadRow {
        private int rowNumber;
        private Long schoolId;
        private Long classRoomId;
        private DayOfWeek dayOfWeek;
        private int periodNumber;
        private String tutorId;
        private Long subjectId;
        private String subjectName;
    }
}
