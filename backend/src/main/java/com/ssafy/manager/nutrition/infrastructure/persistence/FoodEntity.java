package com.ssafy.manager.nutrition.infrastructure.persistence;

import com.ssafy.manager.nutrition.domain.Food;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "foods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FoodEntity {

    @Id
    @Column(name = "food_code", length = 100)
    private String foodCode;

    @Column(nullable = false, length = 300)
    private String name;

    private double caloriesPer100g;
    private double carbsPer100g;
    private double proteinPer100g;
    private double fatPer100g;
    private double fiberPer100g;

    public static FoodEntity from(Food food) {
        return new FoodEntity(
                food.getFoodCode(),
                food.getName(),
                food.getCaloriesPer100g(),
                food.getCarbsPer100g(),
                food.getProteinPer100g(),
                food.getFatPer100g(),
                food.getFiberPer100g()
        );
    }

    public Food toDomain() {
        return new Food(foodCode, name, caloriesPer100g, carbsPer100g, proteinPer100g, fatPer100g, fiberPer100g);
    }
}
