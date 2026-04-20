package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.ClassRoom;
import com.laby.annual.scheduler.entity.School;
import com.laby.annual.scheduler.entity.Subject;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.ClassRoomRepository;
import com.laby.annual.scheduler.repository.SchoolRepository;
import com.laby.annual.scheduler.repository.SubjectRepository;
import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.TutorSubjectRepository;
import com.laby.annual.scheduler.repository.WeeklyTimetableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableGenerationServiceTest {

    @Mock
    private WeeklyTimetableRepository weeklyTimetableRepository;
    @Mock
    private TimetableEntryRepository timetableEntryRepository;
    @Mock
    private AnnualTimetableEntryRepository annualTimetableEntryRepository;
    @Mock
    private ClassRoomRepository classRoomRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TutorSubjectRepository tutorSubjectRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private TutorSelectionService tutorSelectionService;
    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private TimetableGenerationService timetableGenerationService;

    @Test
    void generateAcademicYearTimetable_keepsSameTutorForSameSubjectAcrossClass() {
        Long schoolId = 1L;
        Long subjectId = 101L;
        LocalDate yearStart = LocalDate.of(2026, 3, 1);
        LocalDate yearEnd = LocalDate.of(2027, 3, 31);

        School school = new School();
        school.setId(schoolId);
        school.setPeriodsPerDay(2);

        ClassRoom classRoom = new ClassRoom();
        classRoom.setId(10L);
        classRoom.setSchoolId(schoolId);
        classRoom.setGrade("10");

        Subject maths = new Subject();
        maths.setId(subjectId);
        maths.setName("Maths");
        maths.setSchoolId(schoolId);
        maths.setGrade("10");
        maths.setWeeklyRequiredPeriods(2);

        Tutor canonicalTutor = new Tutor();
        canonicalTutor.setTutorId("T002");
        canonicalTutor.setSchoolId(schoolId);
        canonicalTutor.setActive(true);
        canonicalTutor.setMaxClassesPerDay(8);

        Tutor alternateTutor = new Tutor();
        alternateTutor.setTutorId("T009");
        alternateTutor.setSchoolId(schoolId);
        alternateTutor.setActive(true);
        alternateTutor.setMaxClassesPerDay(8);

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(classRoomRepository.findBySchoolId(schoolId)).thenReturn(List.of(classRoom));
        when(subjectRepository.findBySchoolIdAndGradeAndActiveTrue(schoolId, "10")).thenReturn(List.of(maths));
        when(tutorSubjectRepository.findTutorIdsBySubjectId(subjectId)).thenReturn(List.of("T002", "T009"));
        when(tutorRepository.findById("T002")).thenReturn(Optional.of(canonicalTutor));
        when(tutorRepository.findById("T009")).thenReturn(Optional.of(alternateTutor));
        when(annualTimetableEntryRepository.existsBySchoolIdAndTutorIdAndDayOfWeekAndPeriodNumberAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                eq(schoolId), anyString(), any(DayOfWeek.class), anyInt(), eq(yearEnd), eq(yearStart)
        )).thenReturn(false);
        when(annualTimetableEntryRepository.countBySchoolIdAndTutorIdAndDayOfWeekAndAcademicYearStartLessThanEqualAndAcademicYearEndGreaterThanEqual(
                eq(schoolId), anyString(), any(DayOfWeek.class), eq(yearEnd), eq(yearStart)
        )).thenAnswer(invocation -> "T009".equals(invocation.getArgument(1)) ? 3L : 0L);
        when(annualTimetableEntryRepository.save(any(AnnualTimetableEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        timetableGenerationService.generateAcademicYearTimetable(schoolId, yearStart, yearEnd);

        ArgumentCaptor<AnnualTimetableEntry> captor = ArgumentCaptor.forClass(AnnualTimetableEntry.class);
        verify(annualTimetableEntryRepository, atLeast(1)).save(captor.capture());

        List<AnnualTimetableEntry> mathsAssignments = captor.getAllValues().stream()
                .filter(entry -> entry.getStatus() == AnnualTimetableEntry.Status.ASSIGNED)
                .filter(entry -> subjectId.equals(entry.getSubjectId()))
                .toList();

        assertEquals(2, mathsAssignments.size());
        assertEquals(List.of("T002", "T002"), mathsAssignments.stream().map(AnnualTimetableEntry::getTutorId).toList());
        assertFalse(mathsAssignments.stream().anyMatch(entry -> "T009".equals(entry.getTutorId())));
    }
}

