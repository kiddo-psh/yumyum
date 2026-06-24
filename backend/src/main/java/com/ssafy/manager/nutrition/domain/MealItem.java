package com.ssafy.manager.nutrition.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id")
    private Meal meal;
    private String foodCode;
    private String foodName;
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
        MealItem item = new MealItem(
                amountGrams,
                food.getCaloriesPer100g() * amountGrams / 100,
                food.getCarbsPer100g() * amountGrams / 100,
                food.getProteinPer100g() * amountGrams / 100,
                food.getFatPer100g() * amountGrams / 100,
                food.getFiberPer100g() * amountGrams / 100
        );
        item.foodCode = food.getFoodCode();
        item.foodName = food.getName();
        return item;
    }

    public static MealItem fromAiEstimate(String itemName, double amountGrams,
                                           double kcal, double protein,
                                           double carb, double fat) {
        MealItem item = new MealItem(amountGrams, kcal, carb, protein, fat, 0.0);
        item.foodCode = null;
        item.foodName = itemName;
        return item;
    }

    void bindTo(Meal meal) {
        this.meal = meal;
    }
}
