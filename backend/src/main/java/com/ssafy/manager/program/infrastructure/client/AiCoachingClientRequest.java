package com.ssafy.manager.program.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiCoachingClientRequest(
        @JsonProperty("week_number") int weekNumber,
        @JsonProperty("health_goal") String healthGoal,
        @JsonProperty("daily_nutrition") List<DailyNutritionRecord> dailyNutrition,
        @JsonProperty("target_kcal") double targetKcal,
        @JsonProperty("target_protein_g") double targetProteinG,
        @JsonProperty("target_carb_g") double targetCarbG,
        @JsonProperty("target_fat_g") double targetFatG,
        @JsonProperty("routine_sessions") List<RoutineSessionRecord> routineSessions,
        @JsonProperty("weight_records") List<WeightRecord> weightRecords
) {
    public record DailyNutritionRecord(
            String date,
            double kcal,
            @JsonProperty("protein_g") double proteinG,
            @JsonProperty("carb_g") double carbG,
            @JsonProperty("fat_g") double fatG,
            @JsonProperty("calories_burned") double caloriesBurned
    ) {}

    public record RoutineSessionRecord(
            @JsonProperty("exercise_name") String exerciseName,
            @JsonProperty("successful_sets") int successfulSets,
            @JsonProperty("total_sets") int totalSets,
            @JsonProperty("weight_kg") double weightKg,
            @JsonProperty("session_date") String sessionDate
    ) {}

    public record WeightRecord(
            String date,
            @JsonProperty("weight_kg") double weightKg
    ) {}
}
