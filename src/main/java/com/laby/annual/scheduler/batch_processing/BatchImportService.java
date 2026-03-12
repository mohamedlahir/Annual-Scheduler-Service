package com.laby.annual.scheduler.batch_processing;

import com.laby.annual.scheduler.entity.Subject;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.DTO.TutorExcelDTO;
import com.laby.annual.scheduler.DTO.SubjectExcelDTO;
import com.laby.annual.scheduler.DTO.TutorSubjectExcelDTO;
import com.laby.annual.scheduler.entity.TutorSubject;
import com.laby.annual.scheduler.repository.SubjectRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.TutorSubjectRepository;
import com.laby.annual.scheduler.service.TutorCRUDService;
import com.laby.annual.scheduler.utils.ExcelUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchImportService {
    private final TutorRepository tutorRepository;
    private final TutorSubjectRepository tutorSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final TutorCRUDService tutorCRUDService;

    // =========================================================
    // MAIN ENTRY POINT
    // =========================================================
    @Transactional
    public void importTutors(MultipartFile tutorsFile,
                             MultipartFile tutorSubjectsFile) {

        // ===============================
        // 1️⃣ PARSE EXCEL FILES
        // ===============================
        List<TutorExcelDTO> tutorDTOs =
                ExcelUtil.parseTutors(tutorsFile);

        List<TutorSubjectExcelDTO> tutorSubjectDTOs =
                ExcelUtil.parseTutorSubjects(tutorSubjectsFile);

        if (tutorDTOs.isEmpty()) {
            throw new RuntimeException("Tutors file is empty");
        }

        // ===============================
        // 2️⃣ VALIDATE RELATIONSHIPS
        // ===============================
        validateTutorSubjects(tutorDTOs, tutorSubjectDTOs);

        // ===============================
        // 3️⃣ MAP & SAVE TUTORS
        // ===============================
        Map<String, Tutor> tutorMap = saveTutors(tutorDTOs);

        // ===============================
        // 4️⃣ MAP & SAVE TUTOR SUBJECTS
        // ===============================
        saveTutorSubjects(tutorSubjectDTOs, tutorMap);
    }

    // =========================================================
    // TUTORS ONLY IMPORT
    // =========================================================
    @Transactional
    public void importTutorsOnly(MultipartFile tutorsFile) {
        List<TutorExcelDTO> tutorDTOs = ExcelUtil.parseTutors(tutorsFile);
        if (tutorDTOs.isEmpty()) {
            throw new RuntimeException("Tutors file is empty");
        }
        saveTutors(tutorDTOs);
    }

    // =========================================================
    // TUTOR SUBJECTS ONLY IMPORT
    // =========================================================
    @Transactional
    public void importTutorSubjectsOnly(MultipartFile tutorSubjectsFile) {
        List<TutorSubjectExcelDTO> tutorSubjectDTOs =
                ExcelUtil.parseTutorSubjects(tutorSubjectsFile);

        if (tutorSubjectDTOs.isEmpty()) {
            throw new RuntimeException("TutorSubjects file is empty");
        }

        validateTutorSubjectsAgainstDb(tutorSubjectDTOs);
        saveTutorSubjectsFromDb(tutorSubjectDTOs);
    }

    // =========================================================
    // SUBJECTS IMPORT (with Grade)
    // =========================================================
    @Transactional
    public void importSubjects(MultipartFile subjectsFile) {

        List<SubjectExcelDTO> subjectDTOs =
                ExcelUtil.parseSubjects(subjectsFile);

        if (subjectDTOs.isEmpty()) {
            throw new RuntimeException("Subjects file is empty");
        }

        for (SubjectExcelDTO dto : subjectDTOs) {
            String name = dto.getName();
            Long schoolId = dto.getSchoolId();
            String grade = dto.getGrade();

            if (name == null || name.isBlank()) {
                continue;
            }
            if (schoolId == null) {
                throw new RuntimeException("SchoolId is required for subject: " + name);
            }

            Subject subject = subjectRepository
                    .findBySchoolIdAndNameAndGrade(schoolId, name, grade)
                    .orElseGet(Subject::new);

            subject.setName(name);
            subject.setSchoolId(schoolId);
            subject.setGrade(grade);
            subject.setWeeklyRequiredPeriods(dto.getWeeklyRequiredPeriods());
            subject.setActive(dto.isActive());

            subjectRepository.save(subject);
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================
    private void validateTutorSubjects(List<TutorExcelDTO> tutors,
                                       List<TutorSubjectExcelDTO> subjects) {

        Set<String> tutorCodes =
                tutors.stream()
                        .map(TutorExcelDTO::getTutorCode)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .collect(Collectors.toSet());

        for (TutorSubjectExcelDTO dto : subjects) {

            String tutorId = dto.getTutorId();
            String grade = dto.getGrade();

            if (tutorId == null || tutorId.trim().isEmpty()) {
                throw new RuntimeException(
                        "TutorSubject row has EMPTY tutorCode for subject: "
                                + dto.getSubjectCode()
                );
            }
            if (grade == null || grade.trim().isEmpty()) {
                throw new RuntimeException(
                        "TutorSubject row has EMPTY grade for subject: "
                                + dto.getSubjectCode()
                );
            }

            if (!tutorCodes.contains(tutorId.trim())) {
                throw new RuntimeException(
                        "TutorSubject refers to unknown TutorCode: " + tutorId
                );
            }
        }
    }

    private void validateTutorSubjectsAgainstDb(List<TutorSubjectExcelDTO> subjects) {
        for (TutorSubjectExcelDTO dto : subjects) {
            String tutorId = dto.getTutorId();
            String grade = dto.getGrade();

            if (tutorId == null || tutorId.trim().isEmpty()) {
                throw new RuntimeException(
                        "TutorSubject row has EMPTY tutorCode for subject: "
                                + dto.getSubjectCode()
                );
            }
            if (grade == null || grade.trim().isEmpty()) {
                throw new RuntimeException(
                        "TutorSubject row has EMPTY grade for subject: "
                                + dto.getSubjectCode()
                );
            }

            tutorRepository.findByTutorCode(tutorId.trim())
                    .orElseThrow(() ->
                            new RuntimeException("Tutor not found in DB for tutorCode: " + tutorId)
                    );
        }
    }


    // =========================================================
    // SAVE TUTORS
    // =========================================================
    private Map<String, Tutor> saveTutors(List<TutorExcelDTO> tutorDTOs) {
        Map<String, Tutor> map = new HashMap<>();

        for (TutorExcelDTO dto : tutorDTOs) {
            if (dto.getTutorCode() == null || dto.getTutorCode().trim().isEmpty()) {
                throw new RuntimeException("TutorCode is required in tutors file");
            }
            if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
                throw new RuntimeException("Email is required for tutorCode: " + dto.getTutorCode());
            }
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Password is required for tutorCode: " + dto.getTutorCode());
            }
            if (dto.getRole() == null || dto.getRole().trim().isEmpty()) {
                throw new RuntimeException("Role is required for tutorCode: " + dto.getTutorCode());
            }
            if (dto.getSchoolId() == null) {
                throw new RuntimeException("SchoolId is required for tutorCode: " + dto.getTutorCode());
            }

            Tutor tutor = new Tutor();

            // tutorId (UUID) is generated in TutorCRUDService
            tutor.setTutorCode(dto.getTutorCode().trim());
            tutor.setName(dto.getTutorName());
            tutor.setEmail(dto.getEmail());
            tutor.setPassword(dto.getPassword());
            tutor.setRole(dto.getRole());
            tutor.setSchoolId(dto.getSchoolId());
            tutor.setMaxClassesPerDay(dto.getMaxDailyHours());
            tutor.setFirstName(dto.getFirstName());
            tutor.setLastName(dto.getLastName());
            tutor.setActive(dto.isActive());

            Tutor saved = tutorCRUDService.createTutorInternal(tutor);
            map.put(dto.getTutorCode().trim(), saved);
        }

        return map;
    }


    // =========================================================
    // SAVE TUTOR SUBJECTS
    // =========================================================
    private void saveTutorSubjects(List<TutorSubjectExcelDTO> subjectDTOs,
                                   Map<String, Tutor> tutorMap) {


        tutorSubjectRepository.deleteAll();

        List<TutorSubject> tutorSubjects = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (TutorSubjectExcelDTO dto : subjectDTOs) {
            String tutorId = dto.getTutorId().trim();
            if (tutorId.isEmpty()) {
                continue;
            }
            Tutor tutor = tutorMap.get(tutorId);
            if (tutor == null) {
                throw new RuntimeException("Tutor not found for tutorCode: " + dto.getTutorId());
            }

            // ✅ SUBJECT LOOKUP
            Subject subject = subjectRepository
                    .findBySchoolIdAndNameAndGrade(
                            tutor.getSchoolId(),
                            dto.getSubjectCode(),
                            dto.getGrade()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Subject not found: " + dto.getSubjectCode()
                                            + " (grade " + dto.getGrade() + ")"
                            )
                    );

            String pairKey = tutor.getTutorId() + "|" + subject.getId();
            if (!seenPairs.add(pairKey)) {
                continue; // skip duplicate pair in the same import
            }

            TutorSubject tutorSubject = new TutorSubject();
            tutorSubject.setTutorId(tutor.getTutorId());
            tutorSubject.setSubjectId(subject.getId()); // ✅ FIX

            tutorSubjects.add(tutorSubject);
        }

        tutorSubjectRepository.saveAll(tutorSubjects);
    }

    // =========================================================
    // SAVE TUTOR SUBJECTS (DB LOOKUP)
    // =========================================================
    private void saveTutorSubjectsFromDb(List<TutorSubjectExcelDTO> subjectDTOs) {

        List<TutorSubject> tutorSubjects = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (TutorSubjectExcelDTO dto : subjectDTOs) {
            String tutorId = dto.getTutorId().trim();
            if (tutorId.isEmpty()) {
                continue;
            }

            Tutor tutor = tutorRepository.findByTutorCode(tutorId)
                    .orElseThrow(() ->
                            new RuntimeException("Tutor not found for tutorCode: " + tutorId)
                    );

            Subject subject = subjectRepository
                    .findBySchoolIdAndNameAndGrade(
                            tutor.getSchoolId(),
                            dto.getSubjectCode(),
                            dto.getGrade()
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Subject not found: " + dto.getSubjectCode()
                                            + " (grade " + dto.getGrade() + ")"
                            )
                    );

            String pairKey = tutor.getTutorId() + "|" + subject.getId();
            if (!seenPairs.add(pairKey)) {
                continue;
            }
            if (tutorSubjectRepository.existsByTutorIdAndSubjectId(
                    tutor.getTutorId(), subject.getId())) {
                continue;
            }

            TutorSubject tutorSubject = new TutorSubject();
            tutorSubject.setTutorId(tutor.getTutorId());
            tutorSubject.setSubjectId(subject.getId());
            tutorSubjects.add(tutorSubject);
        }

        tutorSubjectRepository.saveAll(tutorSubjects);
    }


}
