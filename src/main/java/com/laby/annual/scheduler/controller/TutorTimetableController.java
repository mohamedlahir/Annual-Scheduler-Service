package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.entity.TimetableEntry;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.security.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequestMapping("/scheduler/api/tutor/timetable")
@RequiredArgsConstructor
public class TutorTimetableController {

    private final TimetableEntryRepository timetableEntryRepository;
    private final TutorRepository tutorRepository;
    private final JWTService jwtService;

    @GetMapping
    public ResponseEntity<List<TimetableEntry>> getTutorTimetable(
            @RequestParam Long weeklyTimetableId,
            @RequestParam(required = false) String tutorId,
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

        return ResponseEntity.ok(
                timetableEntryRepository
                        .findByWeeklyTimetableIdAndSchoolIdAndTutorIdInOrderByDayOfWeekAscPeriodNumberAsc(
                                weeklyTimetableId,
                                schoolId,
                                new ArrayList<>(tutorCandidates)
                        )
        );
    }

    private Set<String> resolveTutorCandidates(String profileId, String requestedTutorId) {
        Set<String> candidates = new LinkedHashSet<>();

        if (requestedTutorId != null && !requestedTutorId.isBlank()) {
            String normalized = requestedTutorId.trim();
            candidates.add(normalized);
            tutorRepository.findByTutorCode(normalized).map(Tutor::getTutorId).ifPresent(candidates::add);
            tutorRepository.findByTutorId(normalized).map(Tutor::getTutorCode).ifPresent(candidates::add);
        }

        if (profileId != null && !profileId.isBlank()) {
            String normalized = profileId.trim();
            candidates.add(normalized);
            tutorRepository.findByTutorId(normalized).map(Tutor::getTutorCode).ifPresent(candidates::add);
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
