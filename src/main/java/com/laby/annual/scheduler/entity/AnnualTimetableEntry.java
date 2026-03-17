package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Table(
        name = "annual_timetable_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "schoolId",
                                "classRoomId",
                                "dayOfWeek",
                                "periodNumber",
                                "academicYearStart",
                                "academicYearEnd"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AnnualTimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long schoolId;
    private Long classRoomId;

    private Long subjectId;
    private String subjectName;
    private String tutorId;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private int periodNumber;

    private LocalDate academicYearStart;
    private LocalDate academicYearEnd;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private ConflictReason conflictReason;

    private boolean active = true;

    public enum Status {
        ASSIGNED,
        REPLACED,
        CONFLICT,
        PENDING
    }

    public enum ConflictReason {
        NO_SUBJECTS_FOR_GRADE,
        SUBJECT_POOL_EXHAUSTED,
        DAILY_SUBJECT_LIMIT_REACHED,
        NO_TUTOR_AVAILABLE
    }
}
