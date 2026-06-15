package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientRequest;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientResponse;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineAiAdjustService {

    private final RoutineExerciseRepository routineExerciseRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final AiRoutineClient aiRoutineClient;

    @Async
    @Transactional
    public void adjustAndSave(Long routineId) {
        try {
            int currentWeek = routineExerciseRepository.findMaxWeekNumberByRoutineId(routineId);
            List<RoutineExercise> exercises =
                    routineExerciseRepository.findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(
                            routineId, currentWeek);
            if (exercises.isEmpty()) return;

            List<RoutineSession> recentSessions =
                    routineSessionRepository.findTop4ByRoutineIdOrderBySessionDateDesc(routineId);
            List<Long> sessionIds = recentSessions.stream().map(RoutineSession::getId).toList();
            List<SessionSet> allSets = sessionIds.isEmpty()
                    ? List.of()
                    : sessionSetRepository.findBySessionIdIn(sessionIds);
            Map<Long, List<SessionSet>> setsBySession =
                    allSets.stream().collect(Collectors.groupingBy(SessionSet::getSessionId));

            AiRoutineAdjustClientResponse response = aiRoutineClient.adjust(
                    new AiRoutineAdjustClientRequest(
                            routineId, currentWeek,
                            buildExerciseInfos(exercises),
                            buildRecentSessionData(recentSessions, setsBySession)
                    )
            );

            routineExerciseRepository.saveAll(createNextWeekExercises(exercises, response));

        } catch (Exception e) {
            log.warn("루틴 자동 조정 실패 routineId={}: {}", routineId, e.getMessage());
        }
    }

    private List<AiRoutineAdjustClientRequest.ExerciseInfo> buildExerciseInfos(
            List<RoutineExercise> exercises) {
        return exercises.stream()
                .map(ex -> new AiRoutineAdjustClientRequest.ExerciseInfo(
                        ex.getId(), ex.getDayLabel(), ex.getExerciseName(),
                        ex.getTargetSets(), ex.getTargetReps(), ex.getTargetWeightKg(),
                        ex.getOrderIndex()))
                .toList();
    }

    private List<AiRoutineAdjustClientRequest.RecentSession> buildRecentSessionData(
            List<RoutineSession> recentSessions, Map<Long, List<SessionSet>> setsBySession) {
        return recentSessions.stream()
                .map(rs -> {
                    List<SessionSet> sessionSets = setsBySession.getOrDefault(rs.getId(), List.of());
                    Map<Long, List<SessionSet>> byExercise = sessionSets.stream()
                            .collect(Collectors.groupingBy(SessionSet::getExerciseId));
                    List<AiRoutineAdjustClientRequest.SessionSetData> setData = byExercise.entrySet().stream()
                            .map(e -> buildSessionSetData(e.getKey(), e.getValue()))
                            .toList();
                    return new AiRoutineAdjustClientRequest.RecentSession(
                            rs.getSessionDate().toString(), setData);
                })
                .toList();
    }

    private AiRoutineAdjustClientRequest.SessionSetData buildSessionSetData(
            Long exerciseId, List<SessionSet> sets) {
        long completed = sets.stream().filter(SessionSet::isCompleted).count();
        double avgReps = sets.stream().mapToInt(SessionSet::getActualReps).average().orElse(0);
        double avgWeight = sets.stream().mapToDouble(SessionSet::getActualWeightKg).average().orElse(0);
        return new AiRoutineAdjustClientRequest.SessionSetData(
                exerciseId, sets.get(0).getExerciseName(),
                sets.size(), (int) completed, avgReps, avgWeight);
    }

    private List<RoutineExercise> createNextWeekExercises(
            List<RoutineExercise> exercises, AiRoutineAdjustClientResponse response) {
        Map<Long, AiRoutineAdjustClientResponse.Adjustment> adjustMap = response.adjustments().stream()
                .collect(Collectors.toMap(AiRoutineAdjustClientResponse.Adjustment::exerciseId, a -> a));
        return exercises.stream()
                .map(ex -> {
                    AiRoutineAdjustClientResponse.Adjustment adj = adjustMap.get(ex.getId());
                    double w = adj != null ? adj.newWeightKg() : ex.getTargetWeightKg();
                    int s = adj != null ? adj.newSets() : ex.getTargetSets();
                    int r = adj != null ? adj.newReps() : ex.getTargetReps();
                    return RoutineExercise.create(
                            ex.getRoutineId(), ex.getDayLabel(), ex.getExerciseName(),
                            s, r, w, ex.getOrderIndex(), response.nextWeekNumber());
                })
                .toList();
    }
}
