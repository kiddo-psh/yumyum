package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.AiMealPhotoAnalyzeResult;

import java.util.List;

public record PhotoAnalysisResponse(
        List<DetectedItemDto> detectedItems,
        double totalKcal,
        String aiComment
) {
    public record DetectedItemDto(
            String name,
            double estimatedGrams,
            double kcal,
            double proteinG,
            double carbG,
            double fatG
    ) {}

    public static PhotoAnalysisResponse from(AiMealPhotoAnalyzeResult result) {
        List<DetectedItemDto> items = result.detectedItems().stream()
                .map(d -> new DetectedItemDto(
                        d.name(), d.estimatedGrams(), d.kcal(),
                        d.proteinG(), d.carbG(), d.fatG()))
                .toList();
        return new PhotoAnalysisResponse(items, result.totalKcal(), result.aiComment());
    }
}
