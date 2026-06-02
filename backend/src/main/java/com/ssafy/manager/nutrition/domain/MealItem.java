package com.ssafy.manager.nutrition.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double amountGrams;
    private double calories;
    private double carbs;
    private double protein;
    private double fat;
    private double fiber;

    private MealItem(double amountGrams, double calories, double carbs,
                     double protein, double fat, double fiber) {
        this.amountGrams = amountGrams;
        this.calories = calories;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.fiber = fiber;
    }

    public static MealItem from(Food food, double amountGrams) {
        return new MealItem(
                amountGrams,
                food.getCaloriesPer100g() * amountGrams / 100,
                food.getCarbsPer100g() * amountGrams / 100,
                food.getProteinPer100g() * amountGrams / 100,
                food.getFatPer100g() * amountGrams / 100,
                food.getFiberPer100g() * amountGrams / 100
        );
    }
}
