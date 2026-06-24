package com.ssafy.manager.routine.presentation;

import java.time.LocalDate;
import java.util.List;

public record CreateSessionRequest(
        Long routineId,
        LocalDate sessionDate,
        int caloriesBurned,
        List<SetItem> sets
) {
    public record SetItem(
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {}
}
