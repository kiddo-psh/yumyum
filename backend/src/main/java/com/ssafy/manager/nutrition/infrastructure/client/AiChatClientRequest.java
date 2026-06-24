package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiChatClientRequest(
        @JsonProperty("message") String message,
        @JsonProperty("context") MealContext context
) {
    public record MealContext(
            @JsonProperty("total_protein_g")  double totalProteinG,
            @JsonProperty("target_protein_g") double targetProteinG,
            @JsonProperty("total_carb_g")     double totalCarbG,
            @JsonProperty("target_carb_g")    double targetCarbG,
            @JsonProperty("total_fat_g")      double totalFatG,
            @JsonProperty("target_fat_g")     double targetFatG,
            @JsonProperty("total_kcal")       double totalKcal,
            @JsonProperty("target_kcal")      double targetKcal,
            @JsonProperty("health_goal")      String healthGoal
    ) {}
}
