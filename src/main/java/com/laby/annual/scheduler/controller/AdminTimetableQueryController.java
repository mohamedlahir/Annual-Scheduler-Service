package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.DTO.AcademicYearOptionDTO;
import com.laby.annual.scheduler.DTO.ClassTimetableEntryDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.service.AnnualTimetableQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scheduler/admin/timetable")
@RequiredArgsConstructor
public class AdminTimetableQueryController {

    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;
    private final AnnualTimetableQueryService annualTimetableQueryService;
    private final TutorRepository tutorRepository;

    @GetMapping("/years")
    public ResponseEntity<List<AcademicYearOptionDTO>> getAvailableAcademicYears(
            @RequestParam Long schoolId
    ) {
        return ResponseEntity.ok(
                annualTimetableEntryRepository.findDistinctAcademicYearsBySchoolId(schoolId)
        );
    }

    @GetMapping("/class")
    public ResponseEntity<List<ClassTimetableEntryDTO>> getClassTimetable(
            @RequestParam Long schoolId,
            @RequestParam Long classRoomId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate
    ) {
        List<AnnualTimetableEntry> entries = annualTimetableQueryService.getClassTimetable(
                schoolId,
                classRoomId,
                academicYearStart,
                academicYearEnd,
                targetDate
        );
        Map<String, String> tutorNameById = tutorRepository.findBySchoolId(schoolId).stream()
                .collect(Collectors.toMap(
                        Tutor::getTutorId,
                        this::resolveTutorName,
                        (first, second) -> first
                ));
        List<ClassTimetableEntryDTO> response = entries.stream()
                .map(entry -> toClassTimetableEntryDTO(entry, tutorNameById))
                .toList();
        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/conflicts")
    public ResponseEntity<List<AnnualTimetableEntry>> getConflicts(
            @RequestParam Long schoolId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd
    ) {
        return ResponseEntity.ok(
                annualTimetableEntryRepository
                        .findBySchoolIdAndAcademicYearStartAndAcademicYearEndAndStatusAndActiveTrueOrderByClassRoomIdAscDayOfWeekAscPeriodNumberAsc(
                                schoolId,
                                academicYearStart,
                                academicYearEnd,
                                AnnualTimetableEntry.Status.CONFLICT
                        )
        );
    }

    @GetMapping("/conflicts/tutor")
    public ResponseEntity<List<AnnualTimetableEntry>> getTutorConflicts(
            @RequestParam Long schoolId,
            @RequestParam String tutorId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd
    ) {
        Set<String> tutorCandidates = new LinkedHashSet<>();
        String normalized = tutorId.trim();
        tutorCandidates.add(normalized);
        tutorRepository.findByTutorCode(normalized).map(com.laby.annual.scheduler.entity.Tutor::getTutorId).ifPresent(tutorCandidates::add);
        tutorRepository.findByTutorId(normalized).map(com.laby.annual.scheduler.entity.Tutor::getTutorCode).ifPresent(tutorCandidates::add);

        return ResponseEntity.ok(
                annualTimetableEntryRepository
                        .findBySchoolIdAndTutorIdInAndAcademicYearStartAndAcademicYearEndAndStatusAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                                schoolId,
                                new ArrayList<>(tutorCandidates),
                                academicYearStart,
                                academicYearEnd,
                                AnnualTimetableEntry.Status.CONFLICT
                        )
        );
    }

    @GetMapping("/tutor")
    public ResponseEntity<List<AnnualTimetableEntry>> getTutorTimetable(
            @RequestParam Long schoolId,
            @RequestParam String tutorId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate
    ) {
        Set<String> tutorCandidates = new LinkedHashSet<>();
        String normalized = tutorId.trim();
        tutorCandidates.add(normalized);
        tutorRepository.findByTutorCode(normalized).map(com.laby.annual.scheduler.entity.Tutor::getTutorId).ifPresent(tutorCandidates::add);
        tutorRepository.findByTutorId(normalized).map(com.laby.annual.scheduler.entity.Tutor::getTutorCode).ifPresent(tutorCandidates::add);

        return ResponseEntity.ok(
                annualTimetableQueryService.getTutorTimetable(
                        schoolId,
                        new ArrayList<>(tutorCandidates),
                        academicYearStart,
                        academicYearEnd,
                        targetDate
                )
        );
    }

    private ClassTimetableEntryDTO toClassTimetableEntryDTO(AnnualTimetableEntry entry, Map<String, String> tutorNameById) {
        ClassTimetableEntryDTO dto = new ClassTimetableEntryDTO();
        dto.setId(entry.getId());
        dto.setSchoolId(entry.getSchoolId());
        dto.setClassRoomId(entry.getClassRoomId());
        dto.setSubjectId(entry.getSubjectId());
        dto.setSubjectName(entry.getSubjectName());
        dto.setTutorId(entry.getTutorId());
        dto.setTutorName(entry.getTutorId() == null ? null : tutorNameById.get(entry.getTutorId()));
        dto.setDayOfWeek(entry.getDayOfWeek());
        dto.setPeriodNumber(entry.getPeriodNumber());
        dto.setAcademicYearStart(entry.getAcademicYearStart());
        dto.setAcademicYearEnd(entry.getAcademicYearEnd());
        dto.setStatus(entry.getStatus());
        dto.setConflictReason(entry.getConflictReason());
        dto.setActive(entry.isActive());
        return dto;
    }

    private String resolveTutorName(Tutor tutor) {
        if (tutor == null) {
            return null;
        }
        if (tutor.getName() != null && !tutor.getName().isBlank()) {
            return tutor.getName().trim();
        }
        String fullName = ((tutor.getFirstName() == null ? "" : tutor.getFirstName().trim()) + " "
                + (tutor.getLastName() == null ? "" : tutor.getLastName().trim())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (tutor.getTutorCode() != null && !tutor.getTutorCode().isBlank()) {
            return tutor.getTutorCode().trim();
        }
        return Objects.toString(tutor.getTutorId(), null);
    }
}
