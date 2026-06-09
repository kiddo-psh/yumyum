package com.ssafy.manager.routine.presentation;

import java.util.List;

public record CreateManualRoutineRequest(
        Long memberId,
        String name,
        int daysPerWeek,
        List<ExerciseItem> exercises
) {
    public record ExerciseItem(
            String dayLabel,
            String exerciseName,
            int targetSets,
            int targetReps,
            double targetWeightKg,
            int orderIndex
    ) {}
}
