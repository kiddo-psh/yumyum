package com.ssafy.manager.program.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCoachingClientResponse(
        @JsonProperty("ai_comment") String aiComment,
        @JsonProperty("nutrition_summary") String nutritionSummary,
        @JsonProperty("exercise_summary") String exerciseSummary,
        @JsonProperty("goal_summary") String goalSummary,
        @JsonProperty("avg_calorie_rate") double avgCalorieRate,
        @JsonProperty("achievement_days") int achievementDays,
        @JsonProperty("weight_trend") Double weightTrend
) {}
