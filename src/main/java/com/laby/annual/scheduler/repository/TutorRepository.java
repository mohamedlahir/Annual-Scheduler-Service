package com.laby.annual.scheduler.repository;

import com.laby.annual.scheduler.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TutorRepository extends JpaRepository<Tutor, String> {

    List<Tutor> findBySchoolIdAndActiveTrue(Long schoolId);


    List<Tutor> findBySchoolId(Long schoolId);

    boolean existsByTutorIdAndSchoolIdAndActiveTrue(String tutorId, Long schoolId);

    java.util.Optional<Tutor> findByTutorId(String tutorId);
    java.util.Optional<Tutor> findByTutorCode(String tutorCode);

    @Query("select t.tutorId from Tutor t where t.tutorCode = :tutorCode")
    java.util.Optional<String> findTutorIdByTutorCode(String tutorCode);

    @Query("select t.tutorCode from Tutor t where t.tutorId = :tutorId")
    java.util.Optional<String> findTutorCodeByTutorId(String tutorId);

    @Query("select t.maxClassesPerDay from Tutor t where t.tutorId = :tutorId")
    int findMaxClassesPerDay(String tutorId);

}
