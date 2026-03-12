package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.entity.TimetableEntry;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.entity.WeeklyTimetable;
import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.WeeklyTimetableRepository;
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

    private final WeeklyTimetableRepository weeklyTimetableRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    @GetMapping("/class")
    public ResponseEntity<List<TimetableEntry>> getClassTimetable(
            @RequestParam Long schoolId,
            @RequestParam Long classRoomId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStartDate
    ) {
        WeeklyTimetable timetable =
                weeklyTimetableRepository
                        .findBySchoolIdAndWeekStartDate(schoolId, weekStartDate)
                        .orElseThrow();

        return ResponseEntity.ok(
                timetableEntryRepository
                        .findByWeeklyTimetableIdAndClassRoomIdOrderByDayOfWeekAscPeriodNumberAsc(
                                timetable.getId(),
                                classRoomId
                        )
        );
    }

    @GetMapping("/tutor")
    public String hello() {
        return "Hello, Admin Timetable!";
    }
}

