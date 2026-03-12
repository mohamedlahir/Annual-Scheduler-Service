package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.SchoolPeriodConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolPeriodConfigRepository
        extends JpaRepository<SchoolPeriodConfig, Long> {

    List<SchoolPeriodConfig> findBySchoolIdOrderByPeriodNumber(Long schoolId);

}
