package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiMealLastRecommendClientRequest(
        @JsonProperty("total_kcal")       double totalKcal,
        @JsonProperty("total_protein_g")  double totalProteinG,
        @JsonProperty("total_carb_g")     double totalCarbG,
        @JsonProperty("total_fat_g")      double totalFatG,
        @JsonProperty("target_kcal")      double targetKcal,
        @JsonProperty("target_protein_g") double targetProteinG,
        @JsonProperty("target_carb_g")    double targetCarbG,
        @JsonProperty("target_fat_g")     double targetFatG,
        @JsonProperty("meal_count")       int mealCount
) {}
