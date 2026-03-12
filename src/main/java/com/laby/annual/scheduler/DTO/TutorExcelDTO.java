package com.laby.annual.scheduler.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorExcelDTO {

    private String tutorCode;
    private String tutorName;
    private String email;
    private String password;
    private String role;
    private Long schoolId;
    private int maxDailyHours;
    private String firstName;
    private String lastName;
    private boolean active = true;
}
