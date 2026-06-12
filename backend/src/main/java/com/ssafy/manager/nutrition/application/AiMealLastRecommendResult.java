package com.ssafy.manager.nutrition.application;

import java.util.List;

public record AiMealLastRecommendResult(
        List<Recommendation> recommendations,
        String priorityNutrient,
        String aiComment
) {
    public record Recommendation(
            String name, double kcal, double proteinG, double carbG, double fatG, String reason
    ) {}
}
