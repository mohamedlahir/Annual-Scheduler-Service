package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.AnnualTimetableSubstitution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnnualTimetableSubstitutionRepository extends JpaRepository<AnnualTimetableSubstitution, Long> {

    boolean existsByAnnualEntryIdAndSubstitutionDate(Long annualEntryId, LocalDate substitutionDate);

    Optional<AnnualTimetableSubstitution> findByAnnualEntryIdAndSubstitutionDate(Long annualEntryId, LocalDate substitutionDate);

    List<AnnualTimetableSubstitution> findBySchoolIdAndSubstitutionDate(Long schoolId, LocalDate substitutionDate);

    List<AnnualTimetableSubstitution> findBySchoolIdAndClassRoomIdAndSubstitutionDateOrderByPeriodNumberAsc(
            Long schoolId,
            Long classRoomId,
            LocalDate substitutionDate
    );

    boolean existsBySubstituteTutorIdAndSubstitutionDateAndPeriodNumber(
            String substituteTutorId,
            LocalDate substitutionDate,
            Integer periodNumber
    );

    long countBySubstituteTutorIdAndSubstitutionDate(String substituteTutorId, LocalDate substitutionDate);
}


