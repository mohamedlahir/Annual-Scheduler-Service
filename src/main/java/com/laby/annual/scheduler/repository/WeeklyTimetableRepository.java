package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.WeeklyTimetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyTimetableRepository
        extends JpaRepository<WeeklyTimetable, Long> {

    Optional<WeeklyTimetable> findBySchoolIdAndWeekStartDate(
            Long schoolId,
            LocalDate weekStartDate
    );
}
