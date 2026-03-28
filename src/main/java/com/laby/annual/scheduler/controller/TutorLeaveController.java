package com.laby.annual.scheduler.controller;
import com.laby.annual.scheduler.DTO.AnnualLeaveSubstitutionRequestDTO;
import com.laby.annual.scheduler.DTO.TutorLeaveRequestDTO;
import com.laby.annual.scheduler.entity.TutorLeave;
import com.laby.annual.scheduler.repository.TutorLeaveRepository;
import com.laby.annual.scheduler.security.JWTService;
import com.laby.annual.scheduler.service.AnnualTimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler/tutor/leave")
@RequiredArgsConstructor
public class TutorLeaveController {

    private final TutorLeaveRepository tutorLeaveRepository;
    private final AnnualTimetableService annualTimetableService;
    private final JWTService jwtService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> applyLeave(
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization,
            @Valid @RequestBody TutorLeaveRequestDTO request
    ) {
        String token = extractToken(authorization);
        Long schoolId = jwtService.extractSchoolId(token);
        String tutorId = jwtService.extractProfileId(token);

        TutorLeave leave = tutorLeaveRepository
                .findByTutorIdAndFromDateAndToDateAndApprovedTrue(tutorId, request.getFromDate(), request.getToDate())
                .orElseGet(TutorLeave::new);
        leave.setTutorId(tutorId);
        leave.setFromDate(request.getFromDate());
        leave.setToDate(request.getToDate());
        leave.setApproved(true);
        leave.setReason(request.getReason());
        tutorLeaveRepository.save(leave);

        AnnualLeaveSubstitutionRequestDTO substitutionRequest = new AnnualLeaveSubstitutionRequestDTO();
        substitutionRequest.setTutorId(tutorId);
        substitutionRequest.setFromDate(request.getFromDate());
        substitutionRequest.setToDate(request.getToDate());

        return ResponseEntity.ok(annualTimetableService.applyLeaveSubstitution(schoolId, substitutionRequest));
    }

    @GetMapping
    public ResponseEntity<List<TutorLeave>> getMyLeaves(
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization
    ) {
        String token = extractToken(authorization);
        String tutorId = jwtService.extractProfileId(token);
        return ResponseEntity.ok(tutorLeaveRepository.findByTutorIdAndApprovedTrueAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                tutorId,
                java.time.LocalDate.of(1900, 1, 1),
                java.time.LocalDate.of(2999, 12, 31)
        ));
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return authorization.substring(7);
    }
}

