package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(
        name = "school_period_config",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"school_id", "periodNumber"})
        }
)
@Getter @Setter @NoArgsConstructor
public class SchoolPeriodConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long schoolId;

    private int periodNumber;

    private LocalTime startTime;
    private LocalTime endTime;

    private boolean isBreak;
}
