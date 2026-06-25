package com.ssafy.manager.nutrition.presentation.dto;

public record DailySummaryResponse(
        int targetCalories,
        double achievedCalories,
        boolean achieved,
        int currentStreak,
        int maxStreak,
        double totalCarbs,
        double totalProtein,
        double totalFat,
        double targetCarbG,
        double targetProteinG,
        double targetFatG
) {}
