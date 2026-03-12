package com.laby.annual.scheduler.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorSubjectExcelDTO {
    private String tutorId;
    private String subjectCode;
    private String grade;
    private int maxWeeklyPeriods;
}
