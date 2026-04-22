package com.laby.annual.scheduler.DTO;

import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Getter
@Setter
public class ClassTimetableEntryDTO {

    private Long id;
    private Long schoolId;
    private Long classRoomId;
    private Long subjectId;
    private String subjectName;
    private String tutorId;
    private String tutorName;
    private DayOfWeek dayOfWeek;
    private int periodNumber;
    private LocalDate academicYearStart;
    private LocalDate academicYearEnd;
    private AnnualTimetableEntry.Status status;
    private AnnualTimetableEntry.ConflictReason conflictReason;
    private boolean active;
}
