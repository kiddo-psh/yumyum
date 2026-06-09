package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.domain.RoutineExercise;

import java.util.List;

public record RoutineResult(
        Long routineId,
        String name,
        int daysPerWeek,
        boolean aiGenerated,
        List<ExerciseResult> exercises,
        String aiComment
) {
    public record ExerciseResult(
            Long id,
            String dayLabel,
            String exerciseName,
            int targetSets,
            int targetReps,
            double targetWeightKg,
            int orderIndex
    ) {
        public static ExerciseResult from(RoutineExercise ex) {
            return new ExerciseResult(
                    ex.getId(), ex.getDayLabel(), ex.getExerciseName(),
                    ex.getTargetSets(), ex.getTargetReps(), ex.getTargetWeightKg(),
                    ex.getOrderIndex()
            );
        }
    }

    public static RoutineResult from(Routine routine, List<RoutineExercise> exercises, String aiComment) {
        return new RoutineResult(
                routine.getId(),
                routine.getName(),
                routine.getDaysPerWeek(),
                routine.isAiGenerated(),
                exercises.stream().map(ExerciseResult::from).toList(),
                aiComment
        );
    }
}
