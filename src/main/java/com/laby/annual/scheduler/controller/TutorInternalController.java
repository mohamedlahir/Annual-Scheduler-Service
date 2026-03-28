package com.laby.annual.scheduler.controller;

import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler/internal/tutor")
@RequiredArgsConstructor
public class TutorInternalController {

    private final TutorRepository tutorRepository;

    @PostMapping
    public ResponseEntity<Void> registerTutor(@RequestBody Tutor tutor) {
        tutorRepository.save(tutor);
        return ResponseEntity.ok().build();
    }
}

