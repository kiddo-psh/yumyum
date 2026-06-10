package com.ssafy.manager.nutrition.presentation.dto;

public record CalorieBalanceResponse(
        int targetCalories,
        double intakeCalories,
        long burnedCalories,
        double remainingCalories,
        int mealCount,
        boolean lastMealRecommendTrigger
) {}
