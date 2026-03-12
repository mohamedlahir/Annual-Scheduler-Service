package com.laby.annual.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tutors")
@Getter @Setter @NoArgsConstructor
public class Tutor {

    @Id
    @Column(name = "tutor_id")
    private String tutorId; // UUID from auth-service (PK)

    private String email;

    private String tutorCode; // human-readable code like T001

    private Long schoolId;

    private String password;

    private String role;

    private int maxClassesPerDay;

    private String name;

    private String firstName;

    private String lastName;

    private boolean active = true;
}
