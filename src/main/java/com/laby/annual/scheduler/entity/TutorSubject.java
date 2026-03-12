package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tutor_subjects",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tutorId", "subjectId"})
        }
)
@Getter @Setter @NoArgsConstructor
public class TutorSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tutorId;   // UUID from auth-service
    private Long subjectId;
}
