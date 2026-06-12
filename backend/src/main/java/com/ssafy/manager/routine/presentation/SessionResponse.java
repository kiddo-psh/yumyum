package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineSessionResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SessionResponse(
        Long sessionId,
        Long routineId,
        Long memberId,
        LocalDate sessionDate,
        LocalDateTime completedAt,
        int caloriesBurned,
        List<SetResponse> sets
) {
    public record SetResponse(
            Long id,
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {}

    public static SessionResponse from(RoutineSessionResult result) {
        return new SessionResponse(
                result.sessionId(), result.routineId(), result.memberId(),
                result.sessionDate(), result.completedAt(),
                result.caloriesBurned(),
                result.sets().stream()
                        .map(s -> new SetResponse(s.id(), s.exerciseId(), s.exerciseName(),
                                s.setNumber(), s.actualReps(), s.actualWeightKg(), s.completed()))
                        .toList()
        );
    }
}
