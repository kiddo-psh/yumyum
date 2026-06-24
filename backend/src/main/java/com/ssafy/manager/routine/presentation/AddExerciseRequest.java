package com.ssafy.manager.routine.presentation;

public record AddExerciseRequest(
        String dayLabel,
        String exerciseName,
        int targetSets,
        int targetReps,
        double targetWeightKg
) {}
