package com.ssafy.manager.program.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPlanClientRequest(
        String gender,
        int age,
        @JsonProperty("height_cm") double heightCm,
        @JsonProperty("weight_kg") double weightKg,
        @JsonProperty("activity_level") String activityLevel,
        @JsonProperty("health_goal") String healthGoal
) {}
