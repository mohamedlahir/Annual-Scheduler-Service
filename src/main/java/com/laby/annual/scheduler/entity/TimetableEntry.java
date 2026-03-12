package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;

@Entity
@Table(
        name = "timetable_entries",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"weeklyTimetableId", "classRoomId", "dayOfWeek", "periodNumber"}),
                @UniqueConstraint(columnNames = {"weeklyTimetableId", "tutorId", "dayOfWeek", "periodNumber"})
        }
)
@Getter @Setter @NoArgsConstructor
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long weeklyTimetableId;

    private Long schoolId;
    private Long classRoomId;
    private String tutorName;
    private String subjectName;
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private int periodNumber;

    private Long subjectId;
    private String tutorId;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ASSIGNED,
        REPLACED,
        CONFLICT,
        PENDING
    }
}
