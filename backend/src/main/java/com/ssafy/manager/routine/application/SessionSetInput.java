package com.ssafy.manager.routine.application;

public record SessionSetInput(
        Long exerciseId,
        String exerciseName,
        int setNumber,
        int actualReps,
        double actualWeightKg,
        boolean completed
) {}
