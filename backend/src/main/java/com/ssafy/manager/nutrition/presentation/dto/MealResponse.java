package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.domain.MealType;

import java.time.LocalDate;
import java.util.List;

public record MealResponse(
        Long id,
        MealType type,
        LocalDate date,
        List<MealItemResponse> items
) {
    public static MealResponse from(Meal meal) {
        return new MealResponse(
                meal.getId(),
                meal.getType(),
                meal.getDate(),
                meal.getItems().stream().map(MealItemResponse::from).toList()
        );
    }
}
