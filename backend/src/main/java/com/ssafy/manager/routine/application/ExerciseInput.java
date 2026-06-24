package com.ssafy.manager.routine.application;

public record ExerciseInput(
        String dayLabel,
        String exerciseName,
        int targetSets,
        int targetReps,
        double targetWeightKg,
        int orderIndex
) {}
