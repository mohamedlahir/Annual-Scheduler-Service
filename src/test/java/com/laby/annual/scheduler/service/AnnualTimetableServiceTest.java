package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.DTO.AnnualLeaveSubstitutionRequestDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.AnnualTimetableSubstitution;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.AnnualTimetableSubstitutionRepository;
import com.laby.annual.scheduler.repository.ClassRoomRepository;
import com.laby.annual.scheduler.repository.SubjectRepository;
import com.laby.annual.scheduler.repository.TutorLeaveRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.TutorSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualTimetableServiceTest {

    @Mock
    private AnnualTimetableEntryRepository annualTimetableEntryRepository;
    @Mock
    private AnnualTimetableSubstitutionRepository annualTimetableSubstitutionRepository;
    @Mock
    private ClassRoomRepository classRoomRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorLeaveRepository tutorLeaveRepository;
    @Mock
    private TutorSubjectRepository tutorSubjectRepository;

    @InjectMocks
    private AnnualTimetableService annualTimetableService;

    @Test
    void applyLeaveSubstitution_assignsAnotherEligibleTutorOnlyForLeaveDate() {
        Long schoolId = 1L;
        Long subjectId = 101L;
        LocalDate leaveDate = LocalDate.of(2026, 3, 18);

        Tutor absentTutor = new Tutor();
        absentTutor.setTutorId("T002");
        absentTutor.setSchoolId(schoolId);
        absentTutor.setActive(true);
        absentTutor.setMaxClassesPerDay(8);

        Tutor replacementTutor = new Tutor();
        replacementTutor.setTutorId("T009");
        replacementTutor.setSchoolId(schoolId);
        replacementTutor.setActive(true);
        replacementTutor.setMaxClassesPerDay(8);

        AnnualTimetableEntry entry = new AnnualTimetableEntry();
        entry.setId(77L);
        entry.setSchoolId(schoolId);
        entry.setClassRoomId(5L);
        entry.setSubjectId(subjectId);
        entry.setTutorId("T002");
        entry.setDayOfWeek(DayOfWeek.WEDNESDAY);
        entry.setPeriodNumber(2);

        AnnualLeaveSubstitutionRequestDTO request = new AnnualLeaveSubstitutionRequestDTO();
        request.setTutorId("T002");
        request.setFromDate(leaveDate);
        request.setToDate(leaveDate);

        when(tutorRepository.findByTutorId("T002")).thenReturn(Optional.of(absentTutor));
        when(tutorRepository.findBySchoolIdAndActiveTrue(schoolId)).thenReturn(List.of(absentTutor, replacementTutor));
        when(annualTimetableEntryRepository.findBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                schoolId, "T002", DayOfWeek.WEDNESDAY, leaveDate, leaveDate
        )).thenReturn(List.of(entry));
        when(annualTimetableSubstitutionRepository.existsByAnnualEntryIdAndSubstitutionDate(77L, leaveDate)).thenReturn(false);
        when(tutorSubjectRepository.existsByTutorIdAndSubjectId("T009", subjectId)).thenReturn(true);
        when(tutorLeaveRepository.existsByTutorIdAndApprovedTrueAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                "T009", leaveDate, leaveDate
        )).thenReturn(false);
        when(annualTimetableEntryRepository.existsBySchoolIdAndTutorIdAndDayOfWeekAndPeriodNumberAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                schoolId, "T009", DayOfWeek.WEDNESDAY, 2, leaveDate, leaveDate
        )).thenReturn(false);
        when(annualTimetableSubstitutionRepository.existsBySubstituteTutorIdAndSubstitutionDateAndPeriodNumber(
                "T009", leaveDate, 2
        )).thenReturn(false);
        when(annualTimetableEntryRepository.countBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                eq(schoolId), anyString(), eq(DayOfWeek.WEDNESDAY), eq(leaveDate), eq(leaveDate)
        )).thenReturn(0L);
        when(annualTimetableSubstitutionRepository.countBySubstituteTutorIdAndSubstitutionDate("T009", leaveDate)).thenReturn(0L);
        when(annualTimetableSubstitutionRepository.save(any(AnnualTimetableSubstitution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = annualTimetableService.applyLeaveSubstitution(schoolId, request);

        ArgumentCaptor<AnnualTimetableSubstitution> captor = ArgumentCaptor.forClass(AnnualTimetableSubstitution.class);
        verify(annualTimetableSubstitutionRepository).save(captor.capture());

        AnnualTimetableSubstitution saved = captor.getValue();
        assertNotNull(saved);
        assertEquals("T009", saved.getSubstituteTutorId());
        assertEquals(AnnualTimetableSubstitution.Status.ASSIGNED, saved.getStatus());
        assertEquals(1, response.get("assignedCount"));
        assertEquals(0, response.get("conflictCount"));
    }
}


