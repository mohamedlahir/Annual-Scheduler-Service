package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.DTO.AcademicYearOptionDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    List<AnnualTimetableEntry> findBySchoolIdAndClassRoomIdAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
            Long schoolId,
            Long classRoomId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    );

    List<AnnualTimetableEntry> findBySchoolIdAndTutorIdInAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
            Long schoolId,
            List<String> tutorIds,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    );

    List<AnnualTimetableEntry> findBySchoolIdAndTutorIdInAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqualAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
            Long schoolId,
            List<String> tutorIds,
            LocalDate date1,
            LocalDate date2
    );

    List<AnnualTimetableEntry> findByIdIn(List<Long> ids);

    List<AnnualTimetableEntry> findBySchoolIdAndAcademicYearStartAndAcademicYearEndAndActiveTrue(
            Long schoolId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    );

    List<AnnualTimetableEntry> findBySchoolIdAndAcademicYearStartAndAcademicYearEndAndStatusAndActiveTrueOrderByClassRoomIdAscDayOfWeekAscPeriodNumberAsc(
            Long schoolId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd,
            AnnualTimetableEntry.Status status
    );

    @Query("""
        select distinct new com.laby.annual.scheduler.DTO.AcademicYearOptionDTO(
            e.academicYearStart,
            e.academicYearEnd
        )
        from AnnualTimetableEntry e
        where e.schoolId = :schoolId
          and e.active = true
        order by e.academicYearStart desc, e.academicYearEnd desc
    """)
    List<AcademicYearOptionDTO> findDistinctAcademicYearsBySchoolId(Long schoolId);
}
