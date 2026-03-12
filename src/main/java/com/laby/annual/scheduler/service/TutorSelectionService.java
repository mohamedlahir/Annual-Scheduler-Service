package com.laby.annual.scheduler.service;

import com.laby.annual.scheduler.repository.TimetableEntryRepository;
import com.laby.annual.scheduler.repository.TutorLeaveRepository;
import com.laby.annual.scheduler.repository.TutorSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutorSelectionService {

    private final TutorSubjectRepository tutorSubjectRepository;
    private final TutorLeaveRepository tutorLeaveRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    /**
     * Returns tutorId if an eligible tutor exists
     */
    public Optional<String> findEligibleTutor(
            Long schoolId,
            Long classRoomId,
            Long subjectId,
            DayOfWeek dayOfWeek,
            LocalDate weekStartDate,
            Long weeklyTimetableId,
            int periodNumber
    ) {

        LocalDate classDate =
                weekStartDate.plusDays(dayOfWeek.getValue() - 1);

        List<String> tutorIds =
                tutorSubjectRepository.findTutorIdsBySubjectId(subjectId);

        for (String tutorId : tutorIds) {

            boolean onLeave =
                    tutorLeaveRepository
                            .existsByTutorIdAndApprovedTrueAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                                    tutorId,
                                    classDate,
                                    classDate
                            );

            if (onLeave) continue;

            boolean slotBusy =
                    timetableEntryRepository
                            .existsByTutorIdAndWeeklyTimetableIdAndDayOfWeekAndPeriodNumber(
                                    tutorId,
                                    weeklyTimetableId,
                                    dayOfWeek,
                                    periodNumber
                            );

            if (slotBusy) continue;

            long dailyCount =
                    timetableEntryRepository
                            .countByTutorIdAndSchoolIdAndWeeklyTimetableIdAndDayOfWeek(
                                    tutorId,
                                    schoolId,
                                    weeklyTimetableId,
                                    dayOfWeek
                            );

            if (dailyCount >= 5) continue;

            return Optional.of(tutorId);
        }

        return Optional.empty();
    }
}
