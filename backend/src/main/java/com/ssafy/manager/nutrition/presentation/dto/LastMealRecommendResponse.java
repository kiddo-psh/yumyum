package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.AiMealLastRecommendResult;

import java.util.List;

public record LastMealRecommendResponse(
        List<MealRecommendation> recommendations,
        String priorityNutrient,
        String aiComment
) {
    public record MealRecommendation(
            String name, double kcal, double proteinG, double carbG, double fatG, String reason
    ) {}

    public static LastMealRecommendResponse from(AiMealLastRecommendResult result) {
        List<MealRecommendation> recs = result.recommendations().stream()
                .map(r -> new MealRecommendation(r.name(), r.kcal(), r.proteinG(), r.carbG(), r.fatG(), r.reason()))
                .toList();
        return new LastMealRecommendResponse(recs, result.priorityNutrient(), result.aiComment());
    }
}
