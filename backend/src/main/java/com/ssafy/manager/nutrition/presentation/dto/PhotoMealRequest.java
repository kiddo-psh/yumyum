package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.PhotoMealCommand;
import com.ssafy.manager.nutrition.application.PhotoMealItemCommand;
import com.ssafy.manager.nutrition.domain.MealType;

import java.util.List;

public record PhotoMealRequest(
        MealType mealType,
        List<PhotoMealItemRequest> items
) {
    public record PhotoMealItemRequest(
            String name,
            double estimatedGrams,
            double kcal,
            double proteinG,
            double carbG,
            double fatG
    ) {}

    public PhotoMealCommand toCommand(Long memberId) {
        List<PhotoMealItemCommand> cmds = items.stream()
                .map(i -> new PhotoMealItemCommand(
                        i.name(), i.estimatedGrams(), i.kcal(),
                        i.proteinG(), i.carbG(), i.fatG()))
                .toList();
        return new PhotoMealCommand(memberId, mealType, cmds);
    }
}
