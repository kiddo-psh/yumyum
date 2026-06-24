package com.ssafy.manager.routine.application;

import com.ssafy.manager.growth.application.StreakService;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutineSessionService {

    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final RoutineAiAdjustService routineAiAdjustService;
    private final StreakService streakService;

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

    public List<RoutineSessionResult> getMonthSessions(Long memberId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<RoutineSession> sessions =
                routineSessionRepository.findByMemberIdAndSessionDateBetween(memberId, from, to);
        if (sessions.isEmpty()) return List.of();
        List<Long> sessionIds = sessions.stream().map(RoutineSession::getId).toList();
        List<SessionSet> allSets = sessionSetRepository.findBySessionIdIn(sessionIds);
        Map<Long, List<SessionSet>> bySession =
                allSets.stream().collect(Collectors.groupingBy(SessionSet::getSessionId));
        return sessions.stream()
                .map(s -> RoutineSessionResult.from(s, bySession.getOrDefault(s.getId(), List.of())))
                .toList();
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
        streakService.increment(memberId, sessionDate);
        return RoutineSessionResult.from(session, sets);
    }
}
