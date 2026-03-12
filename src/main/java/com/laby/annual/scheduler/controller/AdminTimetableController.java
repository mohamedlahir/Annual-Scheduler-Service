package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.service.TimetableGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/scheduler/api/admin/timetable")
@RequiredArgsConstructor
public class AdminTimetableController {

    private final TimetableGenerationService timetableGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateTimetable(
            @RequestParam Long schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate academicYearStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate academicYearEnd
    ) {
        timetableGenerationService.generateAcademicYearTimetable(
                schoolId,
                academicYearStart,
                academicYearEnd
        );
        return ResponseEntity.ok("Academic year timetable generation triggered");
    }
}
