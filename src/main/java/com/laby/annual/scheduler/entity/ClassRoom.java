package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "class_rooms",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"schoolId", "grade", "section"})
        }
)
@Getter @Setter @NoArgsConstructor
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String grade;
    private String section;

    private Long schoolId;

    private boolean active = true;
}
