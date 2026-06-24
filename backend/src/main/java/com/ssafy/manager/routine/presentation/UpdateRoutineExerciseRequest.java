package com.ssafy.manager.routine.presentation;

public record UpdateRoutineExerciseRequest(
        String exerciseName,
        int targetSets,
        int targetReps,
        double targetWeightKg
) {}
