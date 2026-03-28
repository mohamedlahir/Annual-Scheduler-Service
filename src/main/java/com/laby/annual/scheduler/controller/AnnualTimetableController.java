package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.DTO.AnnualLeaveSubstitutionRequestDTO;
import com.laby.annual.scheduler.security.JWTService;
import com.laby.annual.scheduler.service.AnnualTimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler/annual-timetable")
@RequiredArgsConstructor
public class AnnualTimetableController {

    private final AnnualTimetableService annualTimetableService;
    private final JWTService jwtService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAnnualTimetable(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("file") MultipartFile file,
            @RequestParam("academicYearStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate academicYearStart,
            @RequestParam("academicYearEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate academicYearEnd
    ) {
        Long schoolId = extractSchoolIdFromToken(authorization);
        Map<String, Object> response = annualTimetableService.uploadAnnualTimetable(
                file,
                academicYearStart,
                academicYearEnd,
                schoolId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave/substitute")
    public ResponseEntity<Map<String, Object>> applyAnnualLeaveSubstitution(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AnnualLeaveSubstitutionRequestDTO request
    ) {
        Long schoolId = extractSchoolIdFromToken(authorization);
        Map<String, Object> response = annualTimetableService.applyLeaveSubstitution(schoolId, request);
        System.err.println("Response from service for leave: " + response);
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
