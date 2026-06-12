package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiMealLastRecommendClientResponse(
        @JsonProperty("recommendations")   List<Recommendation> recommendations,
        @JsonProperty("priority_nutrient") String priorityNutrient,
        @JsonProperty("ai_comment")        String aiComment
) {
    public record Recommendation(
            @JsonProperty("name")      String name,
            @JsonProperty("kcal")      double kcal,
            @JsonProperty("protein_g") double proteinG,
            @JsonProperty("carb_g")    double carbG,
            @JsonProperty("fat_g")     double fatG,
            @JsonProperty("reason")    String reason
    ) {}
}
