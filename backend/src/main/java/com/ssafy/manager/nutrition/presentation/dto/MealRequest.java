package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.MealCommand;
import com.ssafy.manager.nutrition.application.MealItemCommand;
import com.ssafy.manager.nutrition.domain.MealType;

import java.time.LocalDate;
import java.util.List;

public record MealRequest(MealType type, LocalDate date, List<Item> items) {

    public record Item(String foodCode, double amountGrams) {}

    public MealCommand toCommand(Long memberId, MealType resolvedType) {
        return new MealCommand(
                memberId,
                resolvedType,
                date,
                items == null ? List.of() : items.stream().map(i -> new MealItemCommand(i.foodCode(), i.amountGrams())).toList()
        );
    }
}
