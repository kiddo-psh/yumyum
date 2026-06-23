package com.ssafy.manager.program.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyCoachingDataService {

    private final MealItemRepository mealItemRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final WeightRepository weightRepository;
    private final ProgramRepository programRepository;

    @Transactional(readOnly = true)
    public AiCoachingClientRequest buildRequest(Long programId, int weekNumber) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new EntityNotFoundException("Program not found: " + programId));
        Long memberId = program.getMemberId();
        LocalDate weekStart = program.getStartDate().plusDays((long) (weekNumber - 1) * 7);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<AiCoachingClientRequest.DailyNutritionRecord> dailyNutrition =
                buildDailyNutrition(memberId, weekStart, weekEnd);

        List<AiCoachingClientRequest.RoutineSessionRecord> routineSessions =
                buildRoutineSessions(memberId, weekStart, weekEnd);

        List<AiCoachingClientRequest.WeightRecord> weightRecords =
                weightRepository.findByMemberIdAndRecordedDateBetween(memberId, weekStart, weekEnd)
                        .stream()
                        .map(w -> new AiCoachingClientRequest.WeightRecord(
                                w.getRecordedDate().toString(), w.getWeightKg()))
                        .toList();

        return new AiCoachingClientRequest(
                weekNumber,
                program.getType().name(),
                dailyNutrition,
                program.getTargetCalories(),
                program.getTargetProteinG(),
                program.getTargetCarbG(),
                program.getTargetFatG(),
                routineSessions,
                weightRecords
        );
    }

    private List<AiCoachingClientRequest.DailyNutritionRecord> buildDailyNutrition(
            Long memberId, LocalDate weekStart, LocalDate weekEnd) {

        Map<LocalDate, MealItemRepository.DailyNutritionSummary> nutritionByDate =
                mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(memberId, weekStart, weekEnd)
                        .stream()
                        .collect(Collectors.toMap(MealItemRepository.DailyNutritionSummary::getDate,
                                s -> s));

        Map<LocalDate, Long> burnByDate =
                routineSessionRepository.findDailyCaloriesBurnedByMemberIdAndDateBetween(memberId, weekStart, weekEnd)
                        .stream()
                        .collect(Collectors.toMap(
                                RoutineSessionRepository.DailyCaloriesSummary::getSessionDate,
                                RoutineSessionRepository.DailyCaloriesSummary::getTotalCalories));

        return weekStart.datesUntil(weekEnd.plusDays(1))
                .map(date -> {
                    var n = nutritionByDate.get(date);
                    long burned = burnByDate.getOrDefault(date, 0L);
                    return new AiCoachingClientRequest.DailyNutritionRecord(
                            date.toString(),
                            n != null ? n.getCalories() : 0.0,
                            n != null ? n.getProtein()  : 0.0,
                            n != null ? n.getCarbs()    : 0.0,
                            n != null ? n.getFat()      : 0.0,
                            (double) burned
                    );
                })
                .toList();
    }

    private List<AiCoachingClientRequest.RoutineSessionRecord> buildRoutineSessions(
            Long memberId, LocalDate weekStart, LocalDate weekEnd) {

        List<RoutineSession> sessions = routineSessionRepository
                .findByMemberIdAndSessionDateBetween(memberId, weekStart, weekEnd);
        if (sessions.isEmpty()) return List.of();

        List<Long> sessionIds = sessions.stream().map(RoutineSession::getId).toList();
        List<SessionSet> sets = sessionSetRepository.findBySessionIdIn(sessionIds);

        Map<Long, LocalDate> sessionDates = sessions.stream()
                .collect(Collectors.toMap(RoutineSession::getId, RoutineSession::getSessionDate));

        return sets.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSessionId() + ":" + s.getExerciseName()))
                .values().stream()
                .map(group -> {
                    SessionSet first = group.get(0);
                    int total = group.size();
                    int successful = (int) group.stream().filter(SessionSet::isCompleted).count();
                    double weightKg = group.stream()
                            .mapToDouble(SessionSet::getActualWeightKg).average().orElse(0.0);
                    String sessionDate = sessionDates
                            .getOrDefault(first.getSessionId(), weekStart).toString();
                    return new AiCoachingClientRequest.RoutineSessionRecord(
                            first.getExerciseName(), successful, total, weightKg, sessionDate);
                })
                .toList();
    }
}
