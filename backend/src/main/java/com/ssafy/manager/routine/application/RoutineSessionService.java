package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientRequest;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientResponse;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineSessionService {

    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final RoutineExerciseRepository routineExerciseRepository;
    private final AiRoutineClient aiRoutineClient;

    @Transactional
    public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                               LocalDate sessionDate,
                                               List<SessionSetInput> setInputs) {
        if (!routineRepository.existsById(routineId)) {
            throw new NoSuchElementException("루틴을 찾을 수 없습니다.");
        }

        RoutineSession session = RoutineSession.create(routineId, memberId, sessionDate);
        routineSessionRepository.save(session);

        List<SessionSet> sets = setInputs.stream()
                .map(i -> SessionSet.create(session.getId(), i.exerciseId(), i.exerciseName(),
                        i.setNumber(), i.actualReps(), i.actualWeightKg(), i.completed()))
                .toList();
        sessionSetRepository.saveAll(sets);

        adjustAndSave(routineId);
        return RoutineSessionResult.from(session, sets);
    }

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

            List<AiRoutineAdjustClientRequest.RecentSession> recentSessionData =
                    recentSessions.stream().map(rs -> {
                        List<SessionSet> sessionSets =
                                setsBySession.getOrDefault(rs.getId(), List.of());
                        Map<Long, List<SessionSet>> byExercise = sessionSets.stream()
                                .collect(Collectors.groupingBy(SessionSet::getExerciseId));
                        List<AiRoutineAdjustClientRequest.SessionSetData> setData =
                                byExercise.entrySet().stream().map(e -> {
                                    List<SessionSet> exSets = e.getValue();
                                    long completed = exSets.stream()
                                            .filter(SessionSet::isCompleted).count();
                                    double avgReps = exSets.stream()
                                            .mapToInt(SessionSet::getActualReps).average().orElse(0);
                                    double avgWeight = exSets.stream()
                                            .mapToDouble(SessionSet::getActualWeightKg).average().orElse(0);
                                    return new AiRoutineAdjustClientRequest.SessionSetData(
                                            e.getKey(), exSets.get(0).getExerciseName(),
                                            exSets.size(), (int) completed, avgReps, avgWeight
                                    );
                                }).toList();
                        return new AiRoutineAdjustClientRequest.RecentSession(
                                rs.getSessionDate().toString(), setData
                        );
                    }).toList();

            List<AiRoutineAdjustClientRequest.ExerciseInfo> exerciseInfos = exercises.stream()
                    .map(ex -> new AiRoutineAdjustClientRequest.ExerciseInfo(
                            ex.getId(), ex.getDayLabel(), ex.getExerciseName(),
                            ex.getTargetSets(), ex.getTargetReps(), ex.getTargetWeightKg(),
                            ex.getOrderIndex()
                    )).toList();

            AiRoutineAdjustClientResponse response = aiRoutineClient.adjust(
                    new AiRoutineAdjustClientRequest(routineId, currentWeek,
                            exerciseInfos, recentSessionData)
            );

            Map<Long, AiRoutineAdjustClientResponse.Adjustment> adjustMap =
                    response.adjustments().stream()
                            .collect(Collectors.toMap(
                                    AiRoutineAdjustClientResponse.Adjustment::exerciseId, a -> a));

            List<RoutineExercise> newExercises = new ArrayList<>();
            for (RoutineExercise ex : exercises) {
                AiRoutineAdjustClientResponse.Adjustment adj = adjustMap.get(ex.getId());
                double w = adj != null ? adj.newWeightKg() : ex.getTargetWeightKg();
                int s = adj != null ? adj.newSets() : ex.getTargetSets();
                int r = adj != null ? adj.newReps() : ex.getTargetReps();
                newExercises.add(RoutineExercise.create(
                        routineId, ex.getDayLabel(), ex.getExerciseName(),
                        s, r, w, ex.getOrderIndex(), response.nextWeekNumber()
                ));
            }
            routineExerciseRepository.saveAll(newExercises);

        } catch (Exception e) {
            log.warn("루틴 자동 조정 실패 routineId={}: {}", routineId, e.getMessage());
        }
    }
}
