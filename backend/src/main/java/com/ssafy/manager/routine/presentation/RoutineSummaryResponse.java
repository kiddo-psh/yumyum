package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineSummaryResult;

public record RoutineSummaryResponse(
        Long routineId,
        String name,
        int daysPerWeek,
        boolean aiGenerated
) {
    public static RoutineSummaryResponse from(RoutineSummaryResult result) {
        return new RoutineSummaryResponse(
                result.routineId(), result.name(),
                result.daysPerWeek(), result.aiGenerated()
        );
    }
}
