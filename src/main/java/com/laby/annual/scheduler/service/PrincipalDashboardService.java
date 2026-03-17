package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.DTO.ClassWorkloadDTO;
import com.laby.annual.scheduler.DTO.PrincipalDashboardResponseDTO;
import com.laby.annual.scheduler.DTO.PrincipalDashboardSummaryDTO;
import com.laby.annual.scheduler.DTO.SubjectWorkloadDTO;
import com.laby.annual.scheduler.DTO.TeacherWorkloadDTO;
import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.ClassRoom;
import com.laby.annual.scheduler.entity.Tutor;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.ClassRoomRepository;
import com.laby.annual.scheduler.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrincipalDashboardService {

    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;
    private final TutorRepository tutorRepository;
    private final ClassRoomRepository classRoomRepository;

    public PrincipalDashboardResponseDTO getDashboard(
            Long schoolId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    ) {
        List<AnnualTimetableEntry> entries =
                annualTimetableEntryRepository.findBySchoolIdAndAcademicYearStartAndAcademicYearEndAndActiveTrue(
                        schoolId,
                        academicYearStart,
                        academicYearEnd
                );

        Map<Long, ClassRoom> classRoomById = classRoomRepository
                .findBySchoolId(schoolId)
                .stream()
                .collect(Collectors.toMap(ClassRoom::getId, c -> c));

        Set<DayOfWeek> activeDays = entries.stream()
                .map(AnnualTimetableEntry::getDayOfWeek)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int activeDayCount = activeDays.size();

        EnumSet<AnnualTimetableEntry.Status> assignedStatuses =
                EnumSet.of(AnnualTimetableEntry.Status.ASSIGNED, AnnualTimetableEntry.Status.REPLACED);

        long assignedPeriods = entries.stream()
                .filter(e -> assignedStatuses.contains(e.getStatus()))
                .count();

        long conflictPeriods = entries.stream()
                .filter(e -> e.getStatus() == AnnualTimetableEntry.Status.CONFLICT)
                .count();

        List<Tutor> tutors = tutorRepository.findBySchoolIdAndActiveTrue(schoolId)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        long totalCapacity = tutors.stream()
                .mapToLong(t -> (long) t.getMaxClassesPerDay() * activeDayCount)
                .sum();

        double avgUtil = totalCapacity == 0
                ? 0.0
                : (assignedPeriods * 100.0) / totalCapacity;

        PrincipalDashboardSummaryDTO summary = new PrincipalDashboardSummaryDTO(
                schoolId,
                academicYearStart,
                academicYearEnd,
                activeDayCount,
                entries.size(),
                assignedPeriods,
                conflictPeriods,
                tutors.size(),
                totalCapacity,
                avgUtil
        );

        Map<String, List<AnnualTimetableEntry>> byTutor = entries.stream()
                .filter(e -> assignedStatuses.contains(e.getStatus()))
                .filter(e -> e.getTutorId() != null && !e.getTutorId().isBlank())
                .collect(Collectors.groupingBy(AnnualTimetableEntry::getTutorId));

        List<TeacherWorkloadDTO> teacherWorkloads = new ArrayList<>();

        for (Tutor tutor : tutors) {
            String tutorId = tutor.getTutorId();
            if (tutorId == null || tutorId.isBlank()) {
                continue;
            }

            List<AnnualTimetableEntry> tutorEntries = byTutor.getOrDefault(tutorId, List.of());
            long tutorAssigned = tutorEntries.size();
            int maxDaily = tutor.getMaxClassesPerDay();
            long capacity = (long) maxDaily * activeDayCount;
            double util = capacity == 0 ? 0.0 : (tutorAssigned * 100.0) / capacity;

            Map<DayOfWeek, Integer> daily = new EnumMap<>(DayOfWeek.class);
            for (DayOfWeek day : activeDays) {
                daily.put(day, 0);
            }
            for (AnnualTimetableEntry entry : tutorEntries) {
                daily.merge(entry.getDayOfWeek(), 1, Integer::sum);
            }

            Map<Long, List<AnnualTimetableEntry>> bySubject = tutorEntries.stream()
                    .filter(e -> e.getSubjectId() != null)
                    .collect(Collectors.groupingBy(AnnualTimetableEntry::getSubjectId));

            List<SubjectWorkloadDTO> subjectWorkloads = new ArrayList<>();
            for (Map.Entry<Long, List<AnnualTimetableEntry>> subjectEntry : bySubject.entrySet()) {
                Long subjectId = subjectEntry.getKey();
                List<AnnualTimetableEntry> subjectEntries = subjectEntry.getValue();
                String subjectName = subjectEntries.get(0).getSubjectName();
                long count = subjectEntries.size();
                double subjectUtil = capacity == 0 ? 0.0 : (count * 100.0) / capacity;
                subjectWorkloads.add(new SubjectWorkloadDTO(subjectId, subjectName, count, subjectUtil));
            }

            Map<Long, List<AnnualTimetableEntry>> byClass = tutorEntries.stream()
                    .filter(e -> e.getClassRoomId() != null)
                    .collect(Collectors.groupingBy(AnnualTimetableEntry::getClassRoomId));

            List<ClassWorkloadDTO> classWorkloads = new ArrayList<>();
            for (Map.Entry<Long, List<AnnualTimetableEntry>> classEntry : byClass.entrySet()) {
                Long classRoomId = classEntry.getKey();
                List<AnnualTimetableEntry> classEntries = classEntry.getValue();
                ClassRoom classRoom = classRoomById.get(classRoomId);
                String classGrade = classRoom != null ? classRoom.getGrade() : null;
                String classSection = classRoom != null ? classRoom.getSection() : null;

                Map<Long, List<AnnualTimetableEntry>> classBySubject = classEntries.stream()
                        .filter(e -> e.getSubjectId() != null)
                        .collect(Collectors.groupingBy(AnnualTimetableEntry::getSubjectId));

                List<SubjectWorkloadDTO> classSubjects = new ArrayList<>();
                for (Map.Entry<Long, List<AnnualTimetableEntry>> subjectEntry : classBySubject.entrySet()) {
                    Long subjectId = subjectEntry.getKey();
                    List<AnnualTimetableEntry> subjectEntries = subjectEntry.getValue();
                    String subjectName = subjectEntries.get(0).getSubjectName();
                    long count = subjectEntries.size();
                    double subjectUtil = classEntries.isEmpty() ? 0.0 : (count * 100.0) / classEntries.size();
                    classSubjects.add(new SubjectWorkloadDTO(subjectId, subjectName, count, subjectUtil));
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
