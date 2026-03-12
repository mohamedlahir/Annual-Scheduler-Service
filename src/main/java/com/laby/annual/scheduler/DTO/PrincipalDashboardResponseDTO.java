package com.laby.annual.scheduler.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrincipalDashboardResponseDTO {

    private PrincipalDashboardSummaryDTO summary;
    private List<TeacherWorkloadDTO> teachers;
}
