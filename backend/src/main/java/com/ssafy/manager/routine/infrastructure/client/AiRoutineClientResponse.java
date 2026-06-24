package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineClientResponse(
        @JsonProperty("routine_name") String routineName,
        @JsonProperty("days") List<Day> days,
        @JsonProperty("ai_comment") String aiComment
) {
    public record Day(
            @JsonProperty("day_label") String dayLabel,
            @JsonProperty("exercises") List<Exercise> exercises
    ) {}

    public record Exercise(
            @JsonProperty("name") String name,
            @JsonProperty("sets") int sets,
            @JsonProperty("reps") int reps,
            @JsonProperty("weight_kg") double weightKg
    ) {}
}
