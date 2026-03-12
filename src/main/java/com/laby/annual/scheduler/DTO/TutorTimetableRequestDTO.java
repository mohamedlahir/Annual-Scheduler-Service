package com.laby.annual.scheduler.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorTimetableRequestDTO {

    private String tutorId;
    private Long weeklyTimetableId;
    private Long schoolId;
}
