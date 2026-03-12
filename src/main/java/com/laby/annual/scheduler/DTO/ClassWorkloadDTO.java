package com.laby.annual.scheduler.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassWorkloadDTO {

    private Long classRoomId;
    private String classGrade;
    private String classSection;
    private long periods;
    private List<SubjectWorkloadDTO> subjects;
}
