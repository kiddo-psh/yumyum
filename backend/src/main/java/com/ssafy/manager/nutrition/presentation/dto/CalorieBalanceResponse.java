package com.ssafy.manager.nutrition.presentation.dto;

public record CalorieBalanceResponse(
        int targetCalories,
        double intakeCalories,
        int burnedCalories,
        double remainingCalories,
        int mealCount,
        boolean lastMealRecommendTrigger
) {}
