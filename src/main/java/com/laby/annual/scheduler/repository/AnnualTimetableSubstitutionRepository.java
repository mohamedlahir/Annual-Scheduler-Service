package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.AnnualTimetableSubstitution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AnnualTimetableSubstitutionRepository extends JpaRepository<AnnualTimetableSubstitution, Long> {

    boolean existsByAnnualEntryIdAndSubstitutionDate(Long annualEntryId, LocalDate substitutionDate);

    boolean existsBySubstituteTutorIdAndSubstitutionDateAndPeriodNumber(
            String substituteTutorId,
            LocalDate substitutionDate,
            Integer periodNumber
    );

    long countBySubstituteTutorIdAndSubstitutionDate(String substituteTutorId, LocalDate substitutionDate);
}


