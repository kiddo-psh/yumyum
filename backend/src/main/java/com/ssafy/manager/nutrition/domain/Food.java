package com.ssafy.manager.nutrition.domain;

import lombok.Getter;

@Getter
public class Food {

    private final String foodCode;
    private final String name;
    private final double caloriesPer100g;
    private final double carbsPer100g;
    private final double proteinPer100g;
    private final double fatPer100g;
    private final double fiberPer100g;

    public Food(String foodCode, String name, double caloriesPer100g, double carbsPer100g,
                double proteinPer100g, double fatPer100g, double fiberPer100g) {
        this.foodCode = foodCode;
        this.name = name;
        this.caloriesPer100g = caloriesPer100g;
        this.carbsPer100g = carbsPer100g;
        this.proteinPer100g = proteinPer100g;
        this.fatPer100g = fatPer100g;
        this.fiberPer100g = fiberPer100g;
    }

    public static Food of(String foodCode, String name, int servingGrams,
                          double calories, double carbs, double protein, double fat, double fiber) {
        double ratio = 100.0 / servingGrams;
        return new Food(foodCode, name,
                calories * ratio,
                carbs * ratio,
                protein * ratio,
                fat * ratio,
                fiber * ratio);
    }
}
