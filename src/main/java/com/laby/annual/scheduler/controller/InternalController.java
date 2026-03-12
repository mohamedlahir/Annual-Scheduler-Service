package com.laby.annual.scheduler.controller.internal;

import com.laby.annual.scheduler.batch_processing.BatchImportService;
import com.laby.annual.scheduler.entity.*;
import com.laby.annual.scheduler.repository.*;
import com.laby.annual.scheduler.service.TutorCRUDService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/scheduler/internal/setup")
@RequiredArgsConstructor
public class InternalController {

    private final SchoolRepository schoolRepository;
    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final TutorRepository tutorRepository;
    private final TutorSubjectRepository tutorSubjectRepository;
    private final TutorAvailabilityRepository tutorAvailabilityRepository;
    private final BatchImportService batchImportService;
    private final TutorCRUDService tutorCRUDService;

    // ================= SCHOOL =================

    @PostMapping("/school")
    public School createSchool(@RequestBody School school) {
        return schoolRepository.save(school);
    }

    // ================= CLASSROOM =================

    @PostMapping("/classroom")
    public ClassRoom createClassRoom(@RequestBody ClassRoom classRoom) {
        return classRoomRepository.save(classRoom);
    }

    // ================= SUBJECT =================

    @PostMapping("/subject")
    public Subject createSubject(@RequestBody Subject subject) {
        return subjectRepository.save(subject);
    }

    // ================= TUTOR =================

    @PostMapping("/tutor")
    public ResponseEntity<String> createTutor(@RequestBody Tutor tutor) {
        return tutorCRUDService.createTutor(tutor);
    }

    @GetMapping("/tutor/getall")
    public Iterable<Tutor> getAllTutors() {
        return tutorRepository.findAll();
    }


    // ================= TUTOR ↔ SUBJECT =================

    @PostMapping("/tutor-subject")
    public TutorSubject mapTutorToSubject(@RequestBody TutorSubject tutorSubject) {
        return tutorSubjectRepository.save(tutorSubject);
    }

    @GetMapping("/tutor/getTutorsSubjects")
    public Iterable<TutorSubject> getAllTutorsSubject() {
        return tutorSubjectRepository.findAll();
    }
    // ================= TUTOR AVAILABILITY =================

    @PostMapping("/tutor-availability")
    public TutorAvailability addTutorAvailability(
            @RequestBody TutorAvailability availability
    ) {
        return tutorAvailabilityRepository.save(availability);
    }


    @PostMapping("/upload/tutors")
    public ResponseEntity<String> uploadTutors(
            @RequestParam MultipartFile tutorsFile,
            @RequestParam MultipartFile tutorSubjectsFile
    ) {
        batchImportService.importTutors(tutorsFile, tutorSubjectsFile);
        return ResponseEntity.ok("Tutors imported successfully");
    }

    @PostMapping("/upload/subjects")
    public ResponseEntity<String> uploadSubjects(
            @RequestParam MultipartFile subjectsFile
    ) {
        batchImportService.importSubjects(subjectsFile);
        return ResponseEntity.ok("Subjects imported successfully");
    }

}
