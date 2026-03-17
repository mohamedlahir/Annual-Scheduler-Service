package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.DTO.AcademicYearOptionDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/scheduler/api/admin/timetable")
@RequiredArgsConstructor
public class AdminTimetableQueryController {

    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;

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
            LocalDate academicYearEnd
    ) {
        return ResponseEntity.ok(
                annualTimetableEntryRepository
                        .findBySchoolIdAndClassRoomIdAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                                schoolId,
                                classRoomId,
                                academicYearStart,
                                academicYearEnd
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
    public String hello() {
        return "Hello, Admin Timetable!";
    }
}
