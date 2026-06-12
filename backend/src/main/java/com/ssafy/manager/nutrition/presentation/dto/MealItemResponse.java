package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.domain.MealItem;

public record MealItemResponse(
        String foodCode,
        String foodName,
        double amountGrams,
        double calories,
        double carbs,
        double protein,
        double fat,
        double fiber
) {
    public static MealItemResponse from(MealItem item) {
        return new MealItemResponse(
                item.getFoodCode(),
                item.getFoodName(),
                item.getAmountGrams(),
                item.getCalories(),
                item.getCarbs(),
                item.getProtein(),
                item.getFat(),
                item.getFiber()
        );
    }
}
