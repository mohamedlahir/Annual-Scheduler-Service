package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.entity.TimetableEntry;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.security.JWTService;
// using explicit constructor instead of Lombok to satisfy static analysis
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/scheduler/tutor/timetable")
public class TutorTimetableController {

    private final TimetableEntryRepository timetableEntryRepository;
    private final TutorRepository tutorRepository;
    private final JWTService jwtService;
    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;

    public TutorTimetableController(
            TimetableEntryRepository timetableEntryRepository,
            TutorRepository tutorRepository,
            JWTService jwtService,
            AnnualTimetableEntryRepository annualTimetableEntryRepository
    ) {
        this.timetableEntryRepository = timetableEntryRepository;
        this.tutorRepository = tutorRepository;
        this.jwtService = jwtService;
        this.annualTimetableEntryRepository = annualTimetableEntryRepository;
    }

    @GetMapping
    public ResponseEntity<List<Object>> getTutorTimetable(
            @RequestParam(required = false) Long weeklyTimetableId,
            @RequestParam(required = false) String tutorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpServletRequest request
    ) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }

        String profileId = jwtService.extractProfileId(token);
        Long schoolId = jwtService.extractSchoolId(token);

        if (schoolId == null) {
            return ResponseEntity.status(403).build();
        }

        Set<String> tutorCandidates = resolveTutorCandidates(profileId, tutorId);
        if (tutorCandidates.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        if (date == null) {
            date = LocalDate.now();
        }

        // First try to fetch annual timetable entries that cover the requested date
        List<AnnualTimetableEntry> annualEntries = annualTimetableEntryRepository
                .findBySchoolIdAndTutorIdInAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqualOrderByDayOfWeekAscPeriodNumberAsc(
                        schoolId,
                        new ArrayList<>(tutorCandidates),
                        date,
                        date
                );

        if (!annualEntries.isEmpty()) {
            List<Object> out = new ArrayList<>(annualEntries.size());
            out.addAll(annualEntries);
            return ResponseEntity.ok(out);
        }

        // Fallback: if no annual entries, use existing weekly timetable repository if weeklyTimetableId provided
        List<TimetableEntry> weeklyEntries = timetableEntryRepository
                .findByWeeklyTimetableIdAndSchoolIdAndTutorIdInOrderByDayOfWeekAscPeriodNumberAsc(
                        weeklyTimetableId,
                        schoolId,
                        new ArrayList<>(tutorCandidates)
                );

        List<Object> out = new ArrayList<>(weeklyEntries.size());
        out.addAll(weeklyEntries);
        return ResponseEntity.ok(out);
    }

    private Set<String> resolveTutorCandidates(String profileId, String requestedTutorId) {
        Set<String> candidates = new LinkedHashSet<>();

        if (requestedTutorId != null && !requestedTutorId.isBlank()) {
            String normalized = requestedTutorId.trim();
            candidates.add(normalized);
            tutorRepository.findTutorIdByTutorCode(normalized).ifPresent(candidates::add);
            tutorRepository.findTutorCodeByTutorId(normalized).ifPresent(candidates::add);
        }

        if (profileId != null && !profileId.isBlank()) {
            String normalized = profileId.trim();
            candidates.add(normalized);
            tutorRepository.findTutorCodeByTutorId(normalized).ifPresent(candidates::add);
        }

        return candidates;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
