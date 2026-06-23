package com.ssafy.manager.nutrition.application;

import java.util.List;

public record AiMealPhotoAnalyzeResult(
        List<DetectedItemResult> detectedItems,
        double totalKcal,
        String aiComment
) {
    public record DetectedItemResult(
            String name,
            double estimatedGrams,
            double kcal,
            double proteinG,
            double carbG,
            double fatG
    ) {}
}
