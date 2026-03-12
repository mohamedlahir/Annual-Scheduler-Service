package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.TutorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface TutorAvailabilityRepository
        extends JpaRepository<TutorAvailability, Long> {

    List<TutorAvailability> findByTutorIdAndDayOfWeekAndAvailableTrue(
            String tutorId,
            DayOfWeek dayOfWeek
    );
}
