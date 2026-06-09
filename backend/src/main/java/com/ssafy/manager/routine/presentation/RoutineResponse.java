package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineResult;

import java.util.List;

public record RoutineResponse(
        Long routineId,
        String name,
        int daysPerWeek,
        boolean aiGenerated,
        List<ExerciseResponse> exercises,
        String aiComment
) {
    public record ExerciseResponse(
            Long id,
            String dayLabel,
            String exerciseName,
            int targetSets,
            int targetReps,
            double targetWeightKg,
            int orderIndex
    ) {
        public static ExerciseResponse from(RoutineResult.ExerciseResult r) {
            return new ExerciseResponse(r.id(), r.dayLabel(), r.exerciseName(),
                    r.targetSets(), r.targetReps(), r.targetWeightKg(), r.orderIndex());
        }
    }

    public static RoutineResponse from(RoutineResult result) {
        return new RoutineResponse(
                result.routineId(),
                result.name(),
                result.daysPerWeek(),
                result.aiGenerated(),
                result.exercises().stream().map(ExerciseResponse::from).toList(),
                result.aiComment()
        );
    }
}
