package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.DTO.*;
import com.laby.annual.scheduler.entity.TimetableEntry;
import com.laby.annual.scheduler.entity.ClassRoom;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.entity.WeeklyTimetable;
import com.laby.annual.scheduler.repository.ClassRoomRepository;
import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import com.laby.annual.scheduler.repository.WeeklyTimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrincipalDashboardService {

    private final WeeklyTimetableRepository weeklyTimetableRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TutorRepository tutorRepository;
    private final ClassRoomRepository classRoomRepository;

    public PrincipalDashboardResponseDTO getDashboard(
            Long schoolId,
            LocalDate weekStartDate
    ) {
        WeeklyTimetable week = weeklyTimetableRepository
                .findBySchoolIdAndWeekStartDate(schoolId, weekStartDate)
                .orElseThrow();

        List<TimetableEntry> entries =
                timetableEntryRepository.findByWeeklyTimetableIdAndSchoolId(
                        week.getId(),
                        schoolId
                );

        Map<Long, ClassRoom> classRoomById = classRoomRepository
                .findBySchoolId(schoolId)
                .stream()
                .collect(Collectors.toMap(ClassRoom::getId, c -> c));

        Set<DayOfWeek> activeDays = entries.stream()
                .map(TimetableEntry::getDayOfWeek)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int activeDayCount = activeDays.size();

        EnumSet<TimetableEntry.Status> assignedStatuses =
                EnumSet.of(TimetableEntry.Status.ASSIGNED, TimetableEntry.Status.REPLACED);

        long assignedPeriods = entries.stream()
                .filter(e -> assignedStatuses.contains(e.getStatus()))
                .count();

        long conflictPeriods = entries.stream()
                .filter(e -> e.getStatus() == TimetableEntry.Status.CONFLICT)
                .count();

        List<Tutor> tutors = tutorRepository.findBySchoolIdAndActiveTrue(schoolId)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        long totalCapacity = tutors.stream()
                .filter(Objects::nonNull)
                .mapToLong(t -> (long) t.getMaxClassesPerDay() * activeDayCount)
                .sum();

        double avgUtil = totalCapacity == 0
                ? 0.0
                : (assignedPeriods * 100.0) / totalCapacity;

        PrincipalDashboardSummaryDTO summary = new PrincipalDashboardSummaryDTO(
                schoolId,
                week.getId(),
                week.getWeekStartDate(),
                week.getWeekEndDate(),
                activeDayCount,
                entries.size(),
                assignedPeriods,
                conflictPeriods,
                tutors.size(),
                totalCapacity,
                avgUtil
        );

        Map<String, List<TimetableEntry>> byTutor = entries.stream()
                .filter(e -> assignedStatuses.contains(e.getStatus()))
                .filter(e -> e.getTutorId() != null && !e.getTutorId().isBlank())
                .collect(Collectors.groupingBy(TimetableEntry::getTutorId));

        List<TeacherWorkloadDTO> teacherWorkloads = new ArrayList<>();

        for (Tutor tutor : tutors) {
            String tutorId = tutor.getTutorId();
            if (tutorId == null || tutorId.isBlank()) {
                continue;
            }
            List<TimetableEntry> tutorEntries = byTutor.getOrDefault(tutorId, List.of());

            long tutorAssigned = tutorEntries.size();
            int maxDaily = tutor.getMaxClassesPerDay();
            long capacity = (long) maxDaily * activeDayCount;
            double util = capacity == 0 ? 0.0 : (tutorAssigned * 100.0) / capacity;

            Map<DayOfWeek, Integer> daily = new EnumMap<>(DayOfWeek.class);
            for (DayOfWeek day : activeDays) {
                daily.put(day, 0);
            }
            for (TimetableEntry e : tutorEntries) {
                daily.merge(e.getDayOfWeek(), 1, Integer::sum);
            }

            Map<Long, List<TimetableEntry>> bySubject =
                    tutorEntries.stream()
                            .filter(e -> e.getSubjectId() != null)
                            .collect(Collectors.groupingBy(TimetableEntry::getSubjectId));

            List<SubjectWorkloadDTO> subjectWorkloads = new ArrayList<>();
            for (Map.Entry<Long, List<TimetableEntry>> entry : bySubject.entrySet()) {
                Long subjectId = entry.getKey();
                List<TimetableEntry> list = entry.getValue();
                String subjectName = list.get(0).getSubjectName();
                long count = list.size();
                double subjectUtil = capacity == 0
                        ? 0.0
                        : (count * 100.0) / capacity;
                subjectWorkloads.add(
                        new SubjectWorkloadDTO(subjectId, subjectName, count, subjectUtil)
                );
            }

                Map<Long, List<TimetableEntry>> byClass =
                        tutorEntries.stream()
                                .filter(e -> e.getClassRoomId() != null)
                                .collect(Collectors.groupingBy(TimetableEntry::getClassRoomId));

                List<ClassWorkloadDTO> classWorkloads = new ArrayList<>();
                for (Map.Entry<Long, List<TimetableEntry>> classEntry : byClass.entrySet()) {
                    Long classRoomId = classEntry.getKey();
                    List<TimetableEntry> classEntries = classEntry.getValue();
                    ClassRoom classRoom = classRoomById.get(classRoomId);
                    String classGrade = classRoom != null ? classRoom.getGrade() : null;
                    String classSection = classRoom != null ? classRoom.getSection() : null;

                Map<Long, List<TimetableEntry>> classBySubject =
                        classEntries.stream()
                                .filter(e -> e.getSubjectId() != null)
                                .collect(Collectors.groupingBy(TimetableEntry::getSubjectId));

                List<SubjectWorkloadDTO> classSubjects = new ArrayList<>();
                for (Map.Entry<Long, List<TimetableEntry>> se : classBySubject.entrySet()) {
                    Long subjectId = se.getKey();
                    List<TimetableEntry> list = se.getValue();
                    String subjectName = list.get(0).getSubjectName();
                    long count = list.size();
                    double subjectUtil = classEntries.isEmpty()
                            ? 0.0
                            : (count * 100.0) / classEntries.size();
                    classSubjects.add(
                            new SubjectWorkloadDTO(subjectId, subjectName, count, subjectUtil)
                    );
                }

                classWorkloads.add(
                        new ClassWorkloadDTO(
                                classRoomId,
                                classGrade,
                                classSection,
                                classEntries.size(),
                                classSubjects
                        )
                );
            }

            teacherWorkloads.add(
                    new TeacherWorkloadDTO(
                            tutorId,
                            tutor.getName(),
                            tutorAssigned,
                            maxDaily,
                            capacity,
                            util,
                            daily,
                            subjectWorkloads,
                            classWorkloads
                    )
            );
        }

        return new PrincipalDashboardResponseDTO(summary, teacherWorkloads);
    }
}
