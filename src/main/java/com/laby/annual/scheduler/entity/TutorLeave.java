package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tutor_leaves")
@Getter @Setter @NoArgsConstructor
public class TutorLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tutorId;

    private LocalDate fromDate;
    private LocalDate toDate;

    private boolean approved;

    private String reason;
}
