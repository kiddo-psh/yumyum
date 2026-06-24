package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineClientRequest(
        @JsonProperty("gender") String gender,
        @JsonProperty("age") int age,
        @JsonProperty("weight_kg") double weightKg,
        @JsonProperty("height_cm") double heightCm,
        @JsonProperty("health_goal") String healthGoal,
        @JsonProperty("has_existing_routine") boolean hasExistingRoutine,
        @JsonProperty("days_per_week") int daysPerWeek,
        @JsonProperty("split_type") String splitType,
        @JsonProperty("split_labels") List<String> splitLabels
) {}
