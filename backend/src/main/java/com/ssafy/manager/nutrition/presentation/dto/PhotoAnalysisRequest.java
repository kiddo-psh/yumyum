package com.ssafy.manager.nutrition.presentation.dto;

public record PhotoAnalysisRequest(
        String imageBase64,
        String mediaType,
        String mealType
) {}
