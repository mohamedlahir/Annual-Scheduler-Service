package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "subjects",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"schoolId", "name", "grade"})
        }
)
@Getter @Setter @NoArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long schoolId;

    private String grade;

    private int weeklyRequiredPeriods;

    private boolean active = true;

//    private String subjectCode;
}
