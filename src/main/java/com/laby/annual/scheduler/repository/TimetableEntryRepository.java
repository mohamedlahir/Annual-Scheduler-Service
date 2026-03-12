package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.util.*;

public interface TimetableEntryRepository
        extends JpaRepository<TimetableEntry, Long> {

    // =========================
    // SLOT / CONFLICT CHECKS
    // =========================

    @Query("""
        select distinct t.tutorId
        from TimetableEntry t
        where t.dayOfWeek = :day
          and t.periodNumber = :period
          and t.tutorId is not null
    """)
    Set<String> findTutorIdsByDayAndPeriod(
            DayOfWeek day,
            int period
    );

    boolean existsByTutorIdAndDayOfWeekAndPeriodNumber(
            String tutorId,
            DayOfWeek dayOfWeek,
            int periodNumber
    );

    boolean existsByTutorIdAndWeeklyTimetableIdAndDayOfWeekAndPeriodNumber(
            String tutorId,
            Long weeklyTimetableId,
            DayOfWeek dayOfWeek,
            int periodNumber
    );

    long countByTutorIdAndSchoolIdAndDayOfWeek(
            String tutorId,
            Long schoolId,
            DayOfWeek dayOfWeek
    );

    long countByTutorIdAndSchoolIdAndWeeklyTimetableIdAndDayOfWeek(
            String tutorId,
            Long schoolId,
            Long weeklyTimetableId,
            DayOfWeek dayOfWeek
    );

    // =========================
    // WEEKLY LOAD (✅ FIXED)
    // =========================

    @Query("""
        select t.tutorId, count(t)
        from TimetableEntry t
        where t.weeklyTimetableId = :weeklyTimetableId
          and t.tutorId in :tutors
        group by t.tutorId
    """)
    List<Object[]> countWeeklyRaw(
            Long weeklyTimetableId,
            List<String> tutors
    );

    default Map<String, Long> countWeeklyLoadByTutors(
            Long weeklyTimetableId,
            List<String> tutors
    ) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] r : countWeeklyRaw(weeklyTimetableId, tutors)) {
            map.put((String) r[0], (Long) r[1]);
        }
        return map;
    }

    // =========================
    // QUERY VIEWS
    // =========================

    List<TimetableEntry> findByWeeklyTimetableIdAndTutorId(
            Long weeklyTimetableId,
            String tutorId
    );

    List<TimetableEntry> findByWeeklyTimetableIdAndTutorIdAndSchoolId(
            Long weeklyTimetableId,
            String tutorId,
            Long schoolId
    );

    List<TimetableEntry>
    findByWeeklyTimetableIdAndSchoolIdAndTutorIdInOrderByDayOfWeekAscPeriodNumberAsc(
            Long weeklyTimetableId,
            Long schoolId,
            List<String> tutorIds
    );

    List<TimetableEntry>
    findByWeeklyTimetableIdAndClassRoomIdOrderByDayOfWeekAscPeriodNumberAsc(
            Long weeklyTimetableId,
            Long classRoomId
    );

    List<TimetableEntry> findByWeeklyTimetableIdAndSchoolId(
            Long weeklyTimetableId,
            Long schoolId
    );
}
