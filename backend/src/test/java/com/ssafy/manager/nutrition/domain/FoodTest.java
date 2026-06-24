package com.ssafy.manager.nutrition.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FoodTest {

    @Test
    void servingSize가_100g이면_영양소_값이_그대로_유지된다() {
        Food food = Food.of("D000001", "닭가슴살", 100, 165.0, 0.0, 31.0, 3.6, 0.0);

        assertThat(food.getCaloriesPer100g()).isEqualTo(165.0);
        assertThat(food.getCarbsPer100g()).isEqualTo(0.0);
        assertThat(food.getProteinPer100g()).isEqualTo(31.0);
        assertThat(food.getFatPer100g()).isEqualTo(3.6);
        assertThat(food.getFiberPer100g()).isEqualTo(0.0);
    }

    @Test
    void servingSize가_200g이면_영양소가_100g_기준으로_절반_정규화된다() {
        Food food = Food.of("D000001", "현미밥", 200, 340.0, 74.0, 6.4, 2.0, 3.0);

        assertThat(food.getCaloriesPer100g()).isCloseTo(170.0, within(0.001));
        assertThat(food.getCarbsPer100g()).isCloseTo(37.0, within(0.001));
        assertThat(food.getProteinPer100g()).isCloseTo(3.2, within(0.001));
        assertThat(food.getFatPer100g()).isCloseTo(1.0, within(0.001));
        assertThat(food.getFiberPer100g()).isCloseTo(1.5, within(0.001));
    }

    @Test
    void foodCode와_name이_그대로_저장된다() {
        Food food = Food.of("D106-266050000-0001", "김치찌개_꽁치", 100, 83.0, 1.19, 7.14, 5.18, 0.70);

        assertThat(food.getFoodCode()).isEqualTo("D106-266050000-0001");
        assertThat(food.getName()).isEqualTo("김치찌개_꽁치");
    }
}
