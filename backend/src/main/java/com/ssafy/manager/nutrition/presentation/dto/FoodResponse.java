package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.domain.Food;

public record FoodResponse(
        String foodCode,
        String name,
        double caloriesPer100g,
        double carbsPer100g,
        double proteinPer100g,
        double fatPer100g,
        double fiberPer100g
) {
    public static FoodResponse from(Food food) {
        return new FoodResponse(
                food.getFoodCode(),
                food.getName(),
                food.getCaloriesPer100g(),
                food.getCarbsPer100g(),
                food.getProteinPer100g(),
                food.getFatPer100g(),
                food.getFiberPer100g()
        );
    }
}
