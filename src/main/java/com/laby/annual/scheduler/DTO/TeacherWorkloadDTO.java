package com.laby.annual.scheduler.DTO;

import lombok.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherWorkloadDTO {

    private String tutorId;
    private String tutorName;
//    private String tutorCode;
    private long assignedPeriods;
    private int maxDailyHours;
    private long capacity;
    private double utilizationPercent;

    private Map<DayOfWeek, Integer> dailyWorkload;
    private List<SubjectWorkloadDTO> subjectWorkloads;
    private List<ClassWorkloadDTO> classWorkloads;
}
