package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.AnnualTimetableSubstitution;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.AnnualTimetableSubstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualTimetableQueryServiceTest {

    @Mock
    private AnnualTimetableEntryRepository annualTimetableEntryRepository;

    @Mock
    private AnnualTimetableSubstitutionRepository annualTimetableSubstitutionRepository;

    private AnnualTimetableQueryService annualTimetableQueryService;

    @BeforeEach
    void setUp() {
        annualTimetableQueryService = new AnnualTimetableQueryService(
                annualTimetableEntryRepository,
                annualTimetableSubstitutionRepository
        );
    }

    @Test
    void classTimetableShouldOverlayAssignedSubstituteForTargetDate() {
        LocalDate academicYearStart = LocalDate.of(2026, 3, 1);
        LocalDate academicYearEnd = LocalDate.of(2027, 3, 31);
        LocalDate targetDate = LocalDate.of(2026, 3, 18);

        AnnualTimetableEntry entry = annualEntry(101L, 2L, "T002", DayOfWeek.WEDNESDAY, 1);
        AnnualTimetableSubstitution substitution = assignedSubstitution(101L, 2L, targetDate, "T002", "T010", 1);

        when(annualTimetableEntryRepository
                .findBySchoolIdAndClassRoomIdAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                        1L, 2L, academicYearStart, academicYearEnd
                ))
                .thenReturn(List.of(entry));
        when(annualTimetableSubstitutionRepository.findBySchoolIdAndClassRoomIdAndSubstitutionDateOrderByPeriodNumberAsc(1L, 2L, targetDate))
                .thenReturn(List.of(substitution));

        List<AnnualTimetableEntry> result = annualTimetableQueryService.getClassTimetable(
                1L,
                2L,
                academicYearStart,
                academicYearEnd,
                targetDate
        );

        assertEquals(1, result.size());
        assertEquals("T010", result.get(0).getTutorId());
        assertEquals(AnnualTimetableEntry.Status.REPLACED, result.get(0).getStatus());
    }

    @Test
    void tutorTimetableShouldHideOriginalTutorAndShowSubstituteForTargetDate() {
        LocalDate academicYearStart = LocalDate.of(2026, 3, 1);
        LocalDate academicYearEnd = LocalDate.of(2027, 3, 31);
        LocalDate targetDate = LocalDate.of(2026, 3, 18);

        AnnualTimetableEntry entry = annualEntry(201L, 2L, "T002", DayOfWeek.WEDNESDAY, 2);
        AnnualTimetableSubstitution substitution = assignedSubstitution(201L, 2L, targetDate, "T002", "T022", 2);

        when(annualTimetableEntryRepository
                .findBySchoolIdAndTutorIdInAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                        1L, List.of("T002"), academicYearStart, academicYearEnd
                ))
                .thenReturn(List.of(entry));
        when(annualTimetableSubstitutionRepository.findBySchoolIdAndSubstitutionDate(1L, targetDate))
                .thenReturn(List.of(substitution));

        List<AnnualTimetableEntry> originalTutorView = annualTimetableQueryService.getTutorTimetable(
                1L,
                List.of("T002"),
                academicYearStart,
                academicYearEnd,
                targetDate
        );

        assertTrue(originalTutorView.isEmpty());

        when(annualTimetableEntryRepository
                .findBySchoolIdAndTutorIdInAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                        1L, List.of("T022"), academicYearStart, academicYearEnd
                ))
                .thenReturn(List.of());
        when(annualTimetableEntryRepository.findByIdIn(List.of(201L))).thenReturn(List.of(entry));

        List<AnnualTimetableEntry> substituteTutorView = annualTimetableQueryService.getTutorTimetable(
                1L,
                List.of("T022"),
                academicYearStart,
                academicYearEnd,
                targetDate
        );

        verify(annualTimetableEntryRepository).findByIdIn(List.of(201L));
        assertEquals(1, substituteTutorView.size());
        assertEquals("T022", substituteTutorView.get(0).getTutorId());
        assertEquals(AnnualTimetableEntry.Status.REPLACED, substituteTutorView.get(0).getStatus());
    }

    private AnnualTimetableEntry annualEntry(Long id, Long classRoomId, String tutorId, DayOfWeek dayOfWeek, int periodNumber) {
        AnnualTimetableEntry entry = new AnnualTimetableEntry();
        entry.setId(id);
        entry.setSchoolId(1L);
        entry.setClassRoomId(classRoomId);
        entry.setSubjectId(3L);
        entry.setSubjectName("Maths");
        entry.setTutorId(tutorId);
        entry.setDayOfWeek(dayOfWeek);
        entry.setPeriodNumber(periodNumber);
        entry.setAcademicYearStart(LocalDate.of(2026, 3, 1));
        entry.setAcademicYearEnd(LocalDate.of(2027, 3, 31));
        entry.setStatus(AnnualTimetableEntry.Status.ASSIGNED);
        entry.setActive(true);
        return entry;
    }

    private AnnualTimetableSubstitution assignedSubstitution(
            Long annualEntryId,
            Long classRoomId,
            LocalDate substitutionDate,
            String originalTutorId,
            String substituteTutorId,
            int periodNumber
    ) {
        AnnualTimetableSubstitution substitution = new AnnualTimetableSubstitution();
        substitution.setAnnualEntryId(annualEntryId);
        substitution.setSchoolId(1L);
        substitution.setClassRoomId(classRoomId);
        substitution.setSubstitutionDate(substitutionDate);
        substitution.setOriginalTutorId(originalTutorId);
        substitution.setSubstituteTutorId(substituteTutorId);
        substitution.setPeriodNumber(periodNumber);
        substitution.setStatus(AnnualTimetableSubstitution.Status.ASSIGNED);
        return substitution;
    }
}

