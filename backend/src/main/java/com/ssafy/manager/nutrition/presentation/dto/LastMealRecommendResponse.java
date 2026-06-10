package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientResponse;

import java.util.List;

public record LastMealRecommendResponse(
        List<MealRecommendation> recommendations,
        String priorityNutrient,
        String aiComment
) {
    public record MealRecommendation(
            String name, double kcal, double proteinG, double carbG, double fatG, String reason
    ) {}

    public static LastMealRecommendResponse from(AiMealLastRecommendClientResponse resp) {
        List<MealRecommendation> recs = resp.recommendations().stream()
                .map(r -> new MealRecommendation(r.name(), r.kcal(), r.proteinG(), r.carbG(), r.fatG(), r.reason()))
                .toList();
        return new LastMealRecommendResponse(recs, resp.priorityNutrient(), resp.aiComment());
    }
}
