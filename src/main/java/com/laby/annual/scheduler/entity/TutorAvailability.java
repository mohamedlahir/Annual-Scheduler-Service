package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;

@Entity
@Table(
        name = "tutor_availability",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tutorId", "dayOfWeek", "periodNumber"})
        }
)
@Getter @Setter @NoArgsConstructor
public class TutorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tutorId;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private int periodNumber;

    private boolean available = true;
}
