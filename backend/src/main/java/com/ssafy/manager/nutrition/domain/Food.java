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
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double caloriesPer100g;
    private double carbsPer100g;
    private double proteinPer100g;
    private double fatPer100g;
    private double fiberPer100g;

    public Food(String name, double caloriesPer100g, double carbsPer100g,
                double proteinPer100g, double fatPer100g, double fiberPer100g) {
        this.name = name;
        this.caloriesPer100g = caloriesPer100g;
        this.carbsPer100g = carbsPer100g;
        this.proteinPer100g = proteinPer100g;
        this.fatPer100g = fatPer100g;
        this.fiberPer100g = fiberPer100g;
    }
}
