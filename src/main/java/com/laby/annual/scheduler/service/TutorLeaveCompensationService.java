package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.entity.TimetableEntry;
import com.laby.annual.scheduler.entity.WeeklyTimetable;
import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.WeeklyTimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutorLeaveCompensationService {

    private final TimetableEntryRepository timetableEntryRepository;
    private final TutorSelectionService tutorSelectionService;
    private final WeeklyTimetableRepository weeklyTimetableRepository;

    public void compensateTutorLeave(
            String tutorId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<WeeklyTimetable> affectedWeeks =
                weeklyTimetableRepository.findAll()
                        .stream()
                        .filter(week ->
                                !(toDate.isBefore(week.getWeekStartDate())
                                        || fromDate.isAfter(week.getWeekEndDate()))
                        )
                        .toList();

        for (WeeklyTimetable week : affectedWeeks) {
            compensateForWeek(week, tutorId, fromDate, toDate);
        }
    }

    private void compensateForWeek(
            WeeklyTimetable week,
            String tutorId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<TimetableEntry> affectedEntries =
                timetableEntryRepository
                        .findByWeeklyTimetableIdAndTutorId(
                                week.getId(),
                                tutorId
                        );

        for (TimetableEntry entry : affectedEntries) {

            LocalDate classDate =
                    week.getWeekStartDate()
                            .plusDays(entry.getDayOfWeek().getValue() - 1);

            if (classDate.isBefore(fromDate) || classDate.isAfter(toDate)) {
                continue;
            }

            // ✅ FIX: expect Optional<String>, not Tutor
            Optional<String> replacementTutorId =
                    tutorSelectionService.findEligibleTutor(
                            entry.getSchoolId(),          // schoolId
                            entry.getClassRoomId(),       // classRoomId
                            entry.getSubjectId(),         // subjectId
                            entry.getDayOfWeek(),         // day
                            week.getWeekStartDate(),      // week start
                            week.getId(),                 // week id
                            entry.getPeriodNumber()       // period
                    );

            if (replacementTutorId.isPresent()) {
                entry.setTutorId(replacementTutorId.get());
                entry.setStatus(TimetableEntry.Status.REPLACED);
            } else {
                entry.setTutorId(null);
                entry.setStatus(TimetableEntry.Status.CONFLICT);
            }

            timetableEntryRepository.save(entry);
        }

        week.setStatus(WeeklyTimetable.Status.ADJUSTED);
        weeklyTimetableRepository.save(week);
    }
}
