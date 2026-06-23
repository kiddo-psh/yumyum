package com.ssafy.manager.nutrition.application;

public record PhotoMealItemCommand(
        String name,
        double estimatedGrams,
        double kcal,
        double proteinG,
        double carbG,
        double fatG
) {}
