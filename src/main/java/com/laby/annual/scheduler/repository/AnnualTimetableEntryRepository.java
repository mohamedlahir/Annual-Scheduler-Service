package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public interface AnnualTimetableEntryRepository extends JpaRepository<AnnualTimetableEntry, Long> {

    void deleteBySchoolIdAndAcademicYearStartAndAcademicYearEnd(
            Long schoolId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    );

    List<AnnualTimetableEntry> findBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
            Long schoolId,
            String tutorId,
            DayOfWeek dayOfWeek,
            LocalDate date1,
            LocalDate date2
    );

    boolean existsBySchoolIdAndTutorIdAndDayOfWeekAndPeriodNumberAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
            Long schoolId,
            String tutorId,
            DayOfWeek dayOfWeek,
            int periodNumber,
            LocalDate date1,
            LocalDate date2
    );

    long countBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
            Long schoolId,
            String tutorId,
            DayOfWeek dayOfWeek,
            LocalDate date1,
            LocalDate date2
    );
}
