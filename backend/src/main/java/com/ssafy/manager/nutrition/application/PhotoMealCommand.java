package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.MealType;

import java.util.List;

public record PhotoMealCommand(
        Long memberId,
        MealType mealType,
        List<PhotoMealItemCommand> items
) {}
