package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.DTO.AcademicYearOptionDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
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
import java.util.Set;

@RestController
@RequestMapping("/scheduler/api/admin/timetable")
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
    public ResponseEntity<List<AnnualTimetableEntry>> getClassTimetable(
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
        return ResponseEntity.ok(
                annualTimetableQueryService.getClassTimetable(
                        schoolId,
                        classRoomId,
                        academicYearStart,
                        academicYearEnd,
                        targetDate
                )
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
}
