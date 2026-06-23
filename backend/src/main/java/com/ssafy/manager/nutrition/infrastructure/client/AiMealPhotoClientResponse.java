package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiMealPhotoClientResponse(
        @JsonProperty("detected_items") List<DetectedItemDto> detectedItems,
        @JsonProperty("total_kcal")     double totalKcal,
        @JsonProperty("ai_comment")     String aiComment
) {
    public record DetectedItemDto(
            String name,
            @JsonProperty("estimated_grams") double estimatedGrams,
            double kcal,
            @JsonProperty("protein_g") double proteinG,
            @JsonProperty("carb_g")    double carbG,
            @JsonProperty("fat_g")     double fatG
    ) {}
}
