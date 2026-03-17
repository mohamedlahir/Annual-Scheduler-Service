package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.DTO.PrincipalDashboardResponseDTO;
import com.laby.annual.scheduler.DTO.PrincipalDashboardSummaryDTO;
import com.laby.annual.scheduler.DTO.TeacherWorkloadDTO;
import com.laby.annual.scheduler.service.PrincipalDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/scheduler/api/principal/dashboard")
@RequiredArgsConstructor
public class PrincipalDashboardController {

    private final PrincipalDashboardService principalDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<PrincipalDashboardResponseDTO> getOverview(
            @RequestParam Long schoolId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd
    ) {
        return ResponseEntity.ok(
                principalDashboardService.getDashboard(schoolId, academicYearStart, academicYearEnd)
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<PrincipalDashboardSummaryDTO> getSummary(
            @RequestParam Long schoolId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd
    ) {
        return ResponseEntity.ok(
                principalDashboardService.getDashboard(schoolId, academicYearStart, academicYearEnd).getSummary()
        );
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<TeacherWorkloadDTO>> getTeachers(
            @RequestParam Long schoolId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate academicYearEnd
    ) {
        return ResponseEntity.ok(
                principalDashboardService.getDashboard(schoolId, academicYearStart, academicYearEnd).getTeachers()
        );
    }
}
