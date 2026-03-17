package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.DTO.AnnualLeaveSubstitutionRequestDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.AnnualTimetableSubstitution;
import com.laby.annual.scheduler.entity.ClassRoom;
import com.laby.annual.scheduler.entity.Subject;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.AnnualTimetableSubstitutionRepository;
import com.laby.annual.scheduler.repository.ClassRoomRepository;
import com.laby.annual.scheduler.repository.SubjectRepository;
import com.laby.annual.scheduler.repository.TutorLeaveRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.TutorSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnnualTimetableService {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "Days", "schoolId", "classId", "TutorId",
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    );

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;
    private final AnnualTimetableSubstitutionRepository annualTimetableSubstitutionRepository;
    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final TutorRepository tutorRepository;
    private final TutorLeaveRepository tutorLeaveRepository;
    private final TutorSubjectRepository tutorSubjectRepository;

    @Transactional
    public Map<String, Object> uploadAnnualTimetable(
            MultipartFile file,
            LocalDate academicYearStart,
            LocalDate academicYearEnd,
            Long schoolIdFromToken
    ) {
        validateDateRange(academicYearStart, academicYearEnd);

        int rowsRead = 0;
        int rowsInserted = 0;

        annualTimetableEntryRepository.deleteBySchoolIdAndAcademicYearStartAndAcademicYearEnd(
                schoolIdFromToken,
                academicYearStart,
                academicYearEnd
        );

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            validateHeader(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                rowsRead++;

                int periodNumber = parsePeriodNumber(getCellString(row.getCell(0)), i);

                Long schoolIdInFile = parseLong(getCellString(row.getCell(1)), "schoolId", i);
                if (!Objects.equals(schoolIdInFile, schoolIdFromToken)) {
                    throw new RuntimeException("Row " + (i + 1) + ": schoolId mismatch with token");
                }

                Long classRoomId = parseLong(getCellString(row.getCell(2)), "classId", i);
                String defaultTutorValue = getCellString(row.getCell(3));
                Subject inferredSubject = inferSubjectForUpload(classRoomId, schoolIdFromToken, defaultTutorValue);

                for (int col = 4; col <= 9; col++) {
                    DayOfWeek day = mapDayByColumn(col);
                    String dayTutorValue = getCellString(row.getCell(col));
                    String tutorValue = isBlank(dayTutorValue) ? defaultTutorValue : dayTutorValue;

                    if (isBlank(tutorValue)) {
                        continue;
                    }

                    String tutorId = resolveTutorId(tutorValue, schoolIdFromToken, i);

                    AnnualTimetableEntry entry = new AnnualTimetableEntry();
                    entry.setSchoolId(schoolIdFromToken);
                    entry.setClassRoomId(classRoomId);
                    if (inferredSubject != null) {
                        entry.setSubjectId(inferredSubject.getId());
                        entry.setSubjectName(inferredSubject.getName());
                    }
                    entry.setTutorId(tutorId);
                    entry.setDayOfWeek(day);
                    entry.setPeriodNumber(periodNumber);
                    entry.setAcademicYearStart(academicYearStart);
                    entry.setAcademicYearEnd(academicYearEnd);
                    entry.setStatus(AnnualTimetableEntry.Status.ASSIGNED);
                    entry.setActive(true);

                    annualTimetableEntryRepository.save(entry);
                    rowsInserted++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read annual timetable excel", e);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schoolId", schoolIdFromToken);
        response.put("academicYearStart", academicYearStart);
        response.put("academicYearEnd", academicYearEnd);
        response.put("rowsRead", rowsRead);
        response.put("slotsInserted", rowsInserted);
        return response;
    }

    @Transactional
    public Map<String, Object> applyLeaveSubstitution(
            Long schoolId,
            AnnualLeaveSubstitutionRequestDTO request
    ) {
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new RuntimeException("fromDate cannot be after toDate");
        }

        String absentTutorId = resolveTutorId(request.getTutorId(), schoolId, -1);
        List<Tutor> tutors = tutorRepository.findBySchoolIdAndActiveTrue(schoolId);

        int assigned = 0;
        int conflicts = 0;

        LocalDate current = request.getFromDate();
        while (!current.isAfter(request.getToDate())) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SUNDAY) {
                List<AnnualTimetableEntry> affectedEntries =
                        annualTimetableEntryRepository
                                .findBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                                        schoolId,
                                        absentTutorId,
                                        day,
                                        current,
                                        current
                                );

                for (AnnualTimetableEntry entry : affectedEntries) {
                    if (annualTimetableSubstitutionRepository.existsByAnnualEntryIdAndSubstitutionDate(entry.getId(), current)) {
                        System.err.println("Affected Entries looped");
                        continue;
                    }

                    Optional<String> replacementTutorId = findReplacementTutor(tutors, entry, current);
                    System.err.println("Replacement tutor search for entry " + entry.getId() + " on " + current + ": " + replacementTutorId);
                    AnnualTimetableSubstitution substitution = new AnnualTimetableSubstitution();
                    substitution.setAnnualEntryId(entry.getId());
                    substitution.setSchoolId(entry.getSchoolId());
                    substitution.setClassRoomId(entry.getClassRoomId());
                    substitution.setPeriodNumber(entry.getPeriodNumber());
                    substitution.setSubstitutionDate(current);
                    substitution.setOriginalTutorId(absentTutorId);

                    if (replacementTutorId.isPresent()) {
                        System.err.println("Replacement tutor Found for entry " + entry.getId() + " on " + current + ": " + replacementTutorId.get());
                        substitution.setSubstituteTutorId(replacementTutorId.get());
                        substitution.setStatus(AnnualTimetableSubstitution.Status.ASSIGNED);
                        assigned++;
                    } else {
                        substitution.setSubstituteTutorId(null);
                        substitution.setStatus(AnnualTimetableSubstitution.Status.CONFLICT);
                        conflicts++;
                    }

                    annualTimetableSubstitutionRepository.save(substitution);
                }
            }

            current = current.plusDays(1);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schoolId", schoolId);
        response.put("absentTutorId", absentTutorId);
        response.put("fromDate", request.getFromDate());
        response.put("toDate", request.getToDate());
        response.put("assignedCount", assigned);
        response.put("conflictCount", conflicts);
        return response;
    }

    private Optional<String> findReplacementTutor(
            List<Tutor> tutors,
            AnnualTimetableEntry entry,
            LocalDate targetDate
    ) {
        DayOfWeek day = targetDate.getDayOfWeek();

        return tutors.stream()
                .filter(t -> t != null && t.getTutorId() != null)
                .filter(t -> !t.getTutorId().equals(entry.getTutorId()))
                .filter(t -> !tutorLeaveRepository.existsByTutorIdAndApprovedTrueAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        t.getTutorId(), targetDate, targetDate
                ))
                .filter(t -> !annualTimetableEntryRepository
                        .existsBySchoolIdAndTutorIdAndDayOfWeekAndPeriodNumberAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                                entry.getSchoolId(),
                                t.getTutorId(),
                                day,
                                entry.getPeriodNumber(),
                                targetDate,
                                targetDate
                        ))
                .filter(t -> !annualTimetableSubstitutionRepository.existsBySubstituteTutorIdAndSubstitutionDateAndPeriodNumber(
                        t.getTutorId(),
                        targetDate,
                        entry.getPeriodNumber()
                ))
                .filter(t -> {
                    long baseLoad = annualTimetableEntryRepository
                            .countBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                                    entry.getSchoolId(),
                                    t.getTutorId(),
                                    day,
                                    targetDate,
                                    targetDate
                            );
                    long substitutionLoad = annualTimetableSubstitutionRepository
                            .countBySubstituteTutorIdAndSubstitutionDate(t.getTutorId(), targetDate);
                    long total = baseLoad + substitutionLoad;
                    return total < t.getMaxClassesPerDay();
                })
                .min(Comparator.comparingLong(t ->
                        annualTimetableEntryRepository
                                .countBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                                        entry.getSchoolId(),
                                        t.getTutorId(),
                                        day,
                                        targetDate,
                                        targetDate
                                )
                                + annualTimetableSubstitutionRepository
                                .countBySubstituteTutorIdAndSubstitutionDate(t.getTutorId(), targetDate)
                ))
                .map(Tutor::getTutorId);
    }

    private Subject inferSubjectForUpload(Long classRoomId, Long schoolId, String tutorValue) {
        if (classRoomId == null || isBlank(tutorValue)) {
            return null;
        }

        Optional<ClassRoom> classRoomOpt = classRoomRepository.findById(classRoomId);
        if (classRoomOpt.isEmpty()) {
            return null;
        }

        String tutorId = resolveTutorId(tutorValue, schoolId, -1);
        List<Long> candidateSubjectIds = tutorSubjectRepository.findByTutorId(tutorId)
                .stream()
                .map(ts -> subjectRepository.findById(ts.getSubjectId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(subject -> Objects.equals(subject.getSchoolId(), schoolId))
                .filter(subject -> Objects.equals(subject.getGrade(), classRoomOpt.get().getGrade()))
                .map(Subject::getId)
                .toList();

        if (candidateSubjectIds.size() != 1) {
            return null;
        }

        return subjectRepository.findById(candidateSubjectIds.get(0)).orElse(null);
    }

    private void validateDateRange(LocalDate academicYearStart, LocalDate academicYearEnd) {
        if (academicYearStart == null || academicYearEnd == null) {
            throw new RuntimeException("academicYearStart and academicYearEnd are required");
        }
        if (academicYearStart.isAfter(academicYearEnd)) {
            throw new RuntimeException("academicYearStart cannot be after academicYearEnd");
        }
    }

    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new RuntimeException("Header row not found in annual timetable excel");
        }

        for (int i = 0; i < REQUIRED_HEADERS.size(); i++) {
            String actual = getCellString(headerRow.getCell(i));
            String expected = REQUIRED_HEADERS.get(i);
            if (!expected.equalsIgnoreCase(actual)) {
                throw new RuntimeException("Invalid annual timetable excel header at column " + (i + 1)
                        + ". expected '" + expected + "' but found '" + actual + "'");
            }
        }
    }

    private DayOfWeek mapDayByColumn(int columnIndex) {
        return switch (columnIndex) {
            case 4 -> DayOfWeek.MONDAY;
            case 5 -> DayOfWeek.TUESDAY;
            case 6 -> DayOfWeek.WEDNESDAY;
            case 7 -> DayOfWeek.THURSDAY;
            case 8 -> DayOfWeek.FRIDAY;
            case 9 -> DayOfWeek.SATURDAY;
            default -> throw new RuntimeException("Unsupported day column index: " + columnIndex);
        };
    }

    private int parsePeriodNumber(String value, int rowIndex) {
        if (isBlank(value)) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": Days (period) is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("P")) {
            normalized = normalized.substring(1);
        }

        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": invalid period value '" + value + "'");
        }
    }

    private Long parseLong(String value, String fieldName, int rowIndex) {
        if (isBlank(value)) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": " + fieldName + " is required");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": invalid " + fieldName + " value '" + value + "'");
        }
    }

    private String resolveTutorId(String tutorValue, Long schoolId, int rowIndex) {
        String normalized = tutorValue == null ? "" : tutorValue.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": tutor value is empty");
        }

        Optional<Tutor> byId = tutorRepository.findByTutorId(normalized);
        if (byId.isPresent() && Objects.equals(byId.get().getSchoolId(), schoolId) && byId.get().isActive()) {
            return byId.get().getTutorId();
        }

        Optional<Tutor> byCode = tutorRepository.findByTutorCode(normalized);
        if (byCode.isPresent() && Objects.equals(byCode.get().getSchoolId(), schoolId) && byCode.get().isActive()) {
            return byCode.get().getTutorId();
        }

        if (rowIndex >= 0) {
            throw new RuntimeException("Row " + (rowIndex + 1) + ": tutor not found or not active for school -> " + normalized);
        }
        throw new RuntimeException("Tutor not found or not active for school -> " + normalized);
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < REQUIRED_HEADERS.size(); c++) {
            if (!isBlank(getCellString(row.getCell(c)))) {
                return false;
            }
        }
        return true;
    }
}
