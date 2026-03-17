package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.entity.AnnualTimetableEntry;
import com.laby.annual.scheduler.entity.AnnualTimetableSubstitution;
import com.laby.annual.scheduler.repository.AnnualTimetableEntryRepository;
import com.laby.annual.scheduler.repository.AnnualTimetableSubstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnualTimetableQueryService {

    private final AnnualTimetableEntryRepository annualTimetableEntryRepository;
    private final AnnualTimetableSubstitutionRepository annualTimetableSubstitutionRepository;

    public List<AnnualTimetableEntry> getClassTimetable(
            Long schoolId,
            Long classRoomId,
            LocalDate academicYearStart,
            LocalDate academicYearEnd,
            LocalDate targetDate
    ) {
        List<AnnualTimetableEntry> entries = annualTimetableEntryRepository
                .findBySchoolIdAndClassRoomIdAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                        schoolId,
                        classRoomId,
                        academicYearStart,
                        academicYearEnd
                );

        if (!shouldApplyDateOverlay(targetDate, academicYearStart, academicYearEnd)) {
            return entries;
        }

        Map<Long, AnnualTimetableSubstitution> substitutionsByEntryId = annualTimetableSubstitutionRepository
                .findBySchoolIdAndClassRoomIdAndSubstitutionDateOrderByPeriodNumberAsc(schoolId, classRoomId, targetDate)
                .stream()
                .collect(Collectors.toMap(
                        AnnualTimetableSubstitution::getAnnualEntryId,
                        substitution -> substitution,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        return entries.stream()
                .map(entry -> applyClassSubstitution(entry, substitutionsByEntryId.get(entry.getId()), targetDate))
                .toList();
    }

    public List<AnnualTimetableEntry> getTutorTimetable(
            Long schoolId,
            Collection<String> tutorCandidates,
            LocalDate academicYearStart,
            LocalDate academicYearEnd,
            LocalDate targetDate
    ) {
        List<String> candidates = tutorCandidates == null
                ? List.of()
                : tutorCandidates.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<AnnualTimetableEntry> baseEntries = annualTimetableEntryRepository
                .findBySchoolIdAndTutorIdInAndAcademicYearStartAndAcademicYearEndAndActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
                        schoolId,
                        candidates,
                        academicYearStart,
                        academicYearEnd
                );

        if (!shouldApplyDateOverlay(targetDate, academicYearStart, academicYearEnd)) {
            return baseEntries;
        }

        List<AnnualTimetableSubstitution> relatedSubstitutions = annualTimetableSubstitutionRepository
                .findBySchoolIdAndSubstitutionDate(schoolId, targetDate)
                .stream()
                .filter(substitution -> candidates.contains(substitution.getOriginalTutorId())
                        || candidates.contains(substitution.getSubstituteTutorId()))
                .toList();

        if (relatedSubstitutions.isEmpty()) {
            return baseEntries;
        }

        Map<Long, AnnualTimetableSubstitution> substitutionByEntryId = relatedSubstitutions.stream()
                .collect(Collectors.toMap(
                        AnnualTimetableSubstitution::getAnnualEntryId,
                        substitution -> substitution,
                        (left, right) -> right,
                        HashMap::new
                ));

        Map<Long, AnnualTimetableEntry> allRelevantEntries = new LinkedHashMap<>();
        for (AnnualTimetableEntry entry : baseEntries) {
            allRelevantEntries.put(entry.getId(), entry);
        }

        Set<Long> missingEntryIds = relatedSubstitutions.stream()
                .filter(substitution -> candidates.contains(substitution.getSubstituteTutorId()))
                .map(AnnualTimetableSubstitution::getAnnualEntryId)
                .filter(id -> !allRelevantEntries.containsKey(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!missingEntryIds.isEmpty()) {
            annualTimetableEntryRepository.findByIdIn(new ArrayList<>(missingEntryIds))
                    .forEach(entry -> allRelevantEntries.put(entry.getId(), entry));
        }

        List<AnnualTimetableEntry> effectiveEntries = new ArrayList<>();
        for (AnnualTimetableEntry entry : allRelevantEntries.values()) {
            if (entry.getDayOfWeek() != targetDate.getDayOfWeek()) {
                if (candidates.contains(entry.getTutorId())) {
                    effectiveEntries.add(copyEntry(entry));
                }
                continue;
            }

            AnnualTimetableSubstitution substitution = substitutionByEntryId.get(entry.getId());
            if (substitution == null) {
                if (candidates.contains(entry.getTutorId())) {
                    effectiveEntries.add(copyEntry(entry));
                }
                continue;
            }

            if (candidates.contains(substitution.getSubstituteTutorId())) {
                effectiveEntries.add(applyAssignedSubstitution(entry, substitution));
            } else if (substitution.getStatus() == AnnualTimetableSubstitution.Status.CONFLICT
                    && candidates.contains(substitution.getOriginalTutorId())) {
                effectiveEntries.add(applyConflictSubstitution(entry));
            }
        }

        effectiveEntries.sort(timetableComparator());
        return effectiveEntries;
    }

    private AnnualTimetableEntry applyClassSubstitution(
            AnnualTimetableEntry entry,
            AnnualTimetableSubstitution substitution,
            LocalDate targetDate
    ) {
        if (substitution == null || entry.getDayOfWeek() != targetDate.getDayOfWeek()) {
            return entry;
        }

        if (substitution.getStatus() == AnnualTimetableSubstitution.Status.ASSIGNED) {
            return applyAssignedSubstitution(entry, substitution);
        }

        return applyConflictSubstitution(entry);
    }

    private AnnualTimetableEntry applyAssignedSubstitution(
            AnnualTimetableEntry entry,
            AnnualTimetableSubstitution substitution
    ) {
        AnnualTimetableEntry copy = copyEntry(entry);
        copy.setTutorId(substitution.getSubstituteTutorId());
        copy.setStatus(AnnualTimetableEntry.Status.REPLACED);
        copy.setConflictReason(null);
        return copy;
    }

    private AnnualTimetableEntry applyConflictSubstitution(AnnualTimetableEntry entry) {
        AnnualTimetableEntry copy = copyEntry(entry);
        copy.setTutorId(null);
        copy.setStatus(AnnualTimetableEntry.Status.CONFLICT);
        copy.setConflictReason(AnnualTimetableEntry.ConflictReason.NO_TUTOR_AVAILABLE);
        return copy;
    }

    private boolean shouldApplyDateOverlay(
            LocalDate targetDate,
            LocalDate academicYearStart,
            LocalDate academicYearEnd
    ) {
        return targetDate != null
                && !targetDate.isBefore(academicYearStart)
                && !targetDate.isAfter(academicYearEnd)
                && targetDate.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private Comparator<AnnualTimetableEntry> timetableComparator() {
        return Comparator
                .comparingInt((AnnualTimetableEntry entry) -> entry.getDayOfWeek() == null ? Integer.MAX_VALUE : entry.getDayOfWeek().getValue())
                .thenComparingInt(AnnualTimetableEntry::getPeriodNumber)
                .thenComparing(entry -> entry.getClassRoomId() == null ? Long.MAX_VALUE : entry.getClassRoomId());
    }

    private AnnualTimetableEntry copyEntry(AnnualTimetableEntry source) {
        AnnualTimetableEntry copy = new AnnualTimetableEntry();
        copy.setId(source.getId());
        copy.setSchoolId(source.getSchoolId());
        copy.setClassRoomId(source.getClassRoomId());
        copy.setSubjectId(source.getSubjectId());
        copy.setSubjectName(source.getSubjectName());
        copy.setTutorId(source.getTutorId());
        copy.setDayOfWeek(source.getDayOfWeek());
        copy.setPeriodNumber(source.getPeriodNumber());
        copy.setAcademicYearStart(source.getAcademicYearStart());
        copy.setAcademicYearEnd(source.getAcademicYearEnd());
        copy.setStatus(source.getStatus());
        copy.setConflictReason(source.getConflictReason());
        copy.setActive(source.isActive());
        return copy;
    }
}

