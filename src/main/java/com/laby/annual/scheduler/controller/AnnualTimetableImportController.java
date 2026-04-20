package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.security.JWTService;
import com.laby.annual.scheduler.service.AnnualTimetableImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler/annual-timetable/import")
@RequiredArgsConstructor
public class AnnualTimetableImportController {

    private final AnnualTimetableImportService annualTimetableImportService;
    private final JWTService jwtService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> importAnnualTimetable(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("file") MultipartFile file,
            @RequestParam("academicYearStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate academicYearStart,
            @RequestParam("academicYearEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate academicYearEnd
    ) {
        Long schoolId = extractSchoolIdFromToken(authorization);
        Map<String, Object> response = annualTimetableImportService.importAnnualTimetable(
                file,
                academicYearStart,
                academicYearEnd,
                schoolId
        );
        return ResponseEntity.ok(response);
    }

    private Long extractSchoolIdFromToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authorization.substring(7);
        Long schoolId = jwtService.extractSchoolId(token);
        if (schoolId == null) {
            throw new RuntimeException("schoolId claim is missing in token");
        }
        return schoolId;
    }
}
