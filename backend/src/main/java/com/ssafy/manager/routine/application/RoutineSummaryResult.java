package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.Routine;

public record RoutineSummaryResult(
        Long routineId,
        String name,
        int daysPerWeek,
        boolean aiGenerated
) {
    public static RoutineSummaryResult from(Routine routine) {
        return new RoutineSummaryResult(
                routine.getId(), routine.getName(),
                routine.getDaysPerWeek(), routine.isAiGenerated()
        );
    }
}
