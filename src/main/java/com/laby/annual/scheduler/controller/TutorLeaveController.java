package com.laby.annual.scheduler.controller;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.entity.TutorLeave;
import com.laby.annual.scheduler.repository.TutorLeaveRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.service.TutorLeaveCompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/scheduler/api/tutor/leave")
@RequiredArgsConstructor
public class TutorLeaveController {

    private final TutorLeaveRepository tutorLeaveRepository;
    private final TutorLeaveCompensationService compensationService;

    @PostMapping
    public ResponseEntity<String> applyLeave(
            @RequestBody TutorLeave leave
    ) {
        leave.setApproved(true); // assume admin approval for now
        tutorLeaveRepository.save(leave);

        // 🔥 Trigger compensation
        compensationService.compensateTutorLeave(
                leave.getTutorId(),
                leave.getFromDate(),
                leave.getToDate()
        );

        return ResponseEntity.ok("Leave applied and timetable compensated");
    }
}

