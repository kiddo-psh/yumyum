package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoutineSessionService {

    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final RoutineAiAdjustService routineAiAdjustService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                               LocalDate sessionDate,
                                               List<SessionSetInput> setInputs) {
        return recordSession(memberId, routineId, sessionDate, 0, setInputs);
    }

    @Transactional
    public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                               LocalDate sessionDate, int caloriesBurned,
                                               List<SessionSetInput> setInputs) {
        if (!routineRepository.existsById(routineId)) {
            throw new NoSuchElementException("루틴을 찾을 수 없습니다.");
        }
        RoutineSessionResult result = saveSessionAndSets(memberId, routineId, sessionDate, caloriesBurned, setInputs);
        routineAiAdjustService.adjustAndSave(routineId);
        return result;
    }

    private RoutineSessionResult saveSessionAndSets(Long memberId, Long routineId,
                                                     LocalDate sessionDate, int caloriesBurned,
                                                     List<SessionSetInput> setInputs) {
        RoutineSession session = RoutineSession.create(routineId, memberId, sessionDate, caloriesBurned);
        routineSessionRepository.save(session);
        List<SessionSet> sets = setInputs.stream()
                .map(i -> SessionSet.create(session.getId(), i.exerciseId(), i.exerciseName(),
                        i.setNumber(), i.actualReps(), i.actualWeightKg(), i.completed()))
                .toList();
        sessionSetRepository.saveAll(sets);
        eventPublisher.publishEvent(new WorkoutLoggedEvent(memberId, sessionDate));
        return RoutineSessionResult.from(session, sets);
    }
}
