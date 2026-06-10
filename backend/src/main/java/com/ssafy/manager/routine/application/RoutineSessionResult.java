package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RoutineSessionResult(
        Long sessionId,
        Long routineId,
        Long memberId,
        LocalDate sessionDate,
        LocalDateTime completedAt,
        List<SetResult> sets
) {
    public record SetResult(
            Long id,
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {
        public static SetResult from(SessionSet s) {
            return new SetResult(s.getId(), s.getExerciseId(), s.getExerciseName(),
                    s.getSetNumber(), s.getActualReps(), s.getActualWeightKg(), s.isCompleted());
        }
    }

    public static RoutineSessionResult from(RoutineSession session, List<SessionSet> sets) {
        return new RoutineSessionResult(
                session.getId(), session.getRoutineId(), session.getMemberId(),
                session.getSessionDate(), session.getCompletedAt(),
                sets.stream().map(SetResult::from).toList()
        );
    }
}
