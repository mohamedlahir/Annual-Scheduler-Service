package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "annual_timetable_substitutions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"annualEntryId", "substitutionDate"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AnnualTimetableSubstitution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long annualEntryId;

    private Long schoolId;

    private Long classRoomId;

    private Integer periodNumber;

    private LocalDate substitutionDate;

    private String originalTutorId;

    private String substituteTutorId;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ASSIGNED,
        CONFLICT
    }
}
