package com.laby.annual.scheduler.utils;

import com.laby.annual.scheduler.DTO.TutorExcelDTO;
import com.laby.annual.scheduler.DTO.SubjectExcelDTO;
import com.laby.annual.scheduler.DTO.TutorSubjectExcelDTO;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    private static final DataFormatter formatter = new DataFormatter();

    // =========================================================
    // PARSE TUTORS FILE
    // =========================================================
    public static List<TutorExcelDTO> parseTutors(MultipartFile file) {

        List<TutorExcelDTO> tutors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            validateTutorHeader(header);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String tutorCode = getString(row, 0);
                if (tutorCode.isBlank()) continue; // ✅ SKIP EMPTY ROWS

                TutorExcelDTO tutor = new TutorExcelDTO();
                tutor.setTutorCode(tutorCode);
                tutor.setTutorName(getString(row, 1));
                tutor.setEmail(getString(row, 2));
                tutor.setPassword(getString(row, 3));
                tutor.setRole(getString(row, 4));
                tutor.setSchoolId(getLong(row, 5));
                tutor.setMaxDailyHours(getInt(row, 6));
                tutor.setFirstName(getString(row, 7));
                tutor.setLastName(getString(row, 8));
                tutor.setActive(getBoolean(row, 9));

                tutors.add(tutor);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tutors excel", e);
        }

        return tutors;
    }

    // =========================================================
    // PARSE TUTOR SUBJECTS FILE ✅ FULL FIX
    // =========================================================
    public static List<TutorSubjectExcelDTO> parseTutorSubjects(MultipartFile file) {

        List<TutorSubjectExcelDTO> subjects = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            validateTutorSubjectHeader(header);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String tutorId = getString(row, 0);
                String subjectCode = getString(row, 1);
                String grade = getString(row, 2);

                // 🚨 HARD GUARD — THIS IS THE FIX
                if (tutorId.isBlank() || subjectCode.isBlank() || grade.isBlank()) {
                    continue; // skip empty / broken rows
                }

                TutorSubjectExcelDTO dto = new TutorSubjectExcelDTO();
                dto.setTutorId(tutorId);
                dto.setSubjectCode(subjectCode);
                dto.setGrade(grade);
                dto.setMaxWeeklyPeriods(getInt(row, 3));

                subjects.add(dto);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tutor-subjects excel", e);
        }

        return subjects;
    }

    // =========================================================
    // PARSE SUBJECTS FILE (with Grade)
    // =========================================================
    public static List<SubjectExcelDTO> parseSubjects(MultipartFile file) {

        List<SubjectExcelDTO> subjects = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            validateSubjectHeader(header);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getString(row, 0);
                if (name.isBlank()) continue;

                SubjectExcelDTO dto = new SubjectExcelDTO();
                dto.setName(name);
                dto.setSchoolId(getLong(row, 1));
                dto.setGrade(getString(row, 2));
                dto.setWeeklyRequiredPeriods(getInt(row, 3));
                dto.setActive(getBoolean(row, 4));

                subjects.add(dto);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse subjects excel", e);
        }

        return subjects;
    }

    // =========================================================
    // HEADER VALIDATION
    // =========================================================
    private static void validateTutorHeader(Row header) {
        if (header == null ||
                !"TutorCode".equalsIgnoreCase(getString(header, 0)) ||
                !"TutorName".equalsIgnoreCase(getString(header, 1)) ||
                !"Email".equalsIgnoreCase(getString(header, 2)) ||
                !"Password".equalsIgnoreCase(getString(header, 3)) ||
                !"Role".equalsIgnoreCase(getString(header, 4)) ||
                !"SchoolId".equalsIgnoreCase(getString(header, 5)) ||
                !"MaxDailyHours".equalsIgnoreCase(getString(header, 6)) ||
                !"FirstName".equalsIgnoreCase(getString(header, 7)) ||
                !"LastName".equalsIgnoreCase(getString(header, 8)) ||
                !"Active".equalsIgnoreCase(getString(header, 9))) {

            throw new RuntimeException("Invalid Tutors Excel header");
        }
    }

    private static void validateTutorSubjectHeader(Row header) {
        if (header == null ||
                !"TutorCode".equalsIgnoreCase(getString(header, 0)) ||
                !"SubjectCode".equalsIgnoreCase(getString(header, 1)) ||
                !"Grade".equalsIgnoreCase(getString(header, 2)) ||
                !"MaxWeeklyPeriods".equalsIgnoreCase(getString(header, 3))) {

            throw new RuntimeException("Invalid TutorSubjects Excel header");
        }
    }

    private static void validateSubjectHeader(Row header) {
        if (header == null ||
                !"SubjectName".equalsIgnoreCase(getString(header, 0)) ||
                !"SchoolId".equalsIgnoreCase(getString(header, 1)) ||
                !"Grade".equalsIgnoreCase(getString(header, 2)) ||
                !"WeeklyRequiredPeriods".equalsIgnoreCase(getString(header, 3)) ||
                !"Active".equalsIgnoreCase(getString(header, 4))) {

            throw new RuntimeException("Invalid Subjects Excel header");
        }
    }

    // =========================================================
    // SAFE CELL READERS
    // =========================================================
    private static String getString(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static int getInt(Row row, int index) {
        String value = getString(row, index);
        if (value.isEmpty()) return 0;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number: " + value);
        }
    }

    private static Long getLong(Row row, int index) {
        String value = getString(row, index);
        if (value.isEmpty()) return null;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number: " + value);
        }
    }

    private static boolean getBoolean(Row row, int index) {
        String value = getString(row, index).toLowerCase();
        if (value.isEmpty()) return true;
        return value.equals("true") || value.equals("1") || value.equals("yes");
    }
}
