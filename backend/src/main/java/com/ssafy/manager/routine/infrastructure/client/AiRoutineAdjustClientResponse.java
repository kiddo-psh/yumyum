package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineAdjustClientResponse(
        @JsonProperty("adjustments") List<Adjustment> adjustments,
        @JsonProperty("ai_comment") String aiComment,
        @JsonProperty("next_week_number") int nextWeekNumber
) {
    public record Adjustment(
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("action") String action,
            @JsonProperty("new_weight_kg") double newWeightKg,
            @JsonProperty("new_sets") int newSets,
            @JsonProperty("new_reps") int newReps,
            @JsonProperty("reason") String reason
    ) {}
}
