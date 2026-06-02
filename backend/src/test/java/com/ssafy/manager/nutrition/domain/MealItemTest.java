package com.ssafy.manager.nutrition.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealItemTest {

    private final Food 닭가슴살 = new Food("닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);

    @Test
    void 섭취량이_100g이면_Food의_per100g_영양소와_동일하다() {
        MealItem item = MealItem.from(닭가슴살, 100.0);

        assertThat(item.getCalories()).isEqualTo(165.0);
        assertThat(item.getCarbs()).isEqualTo(0.0);
        assertThat(item.getProtein()).isEqualTo(31.0);
        assertThat(item.getFat()).isEqualTo(3.6);
        assertThat(item.getFiber()).isEqualTo(0.0);
    }

    @Test
    void 섭취량이_200g이면_영양소가_2배로_계산된다() {
        MealItem item = MealItem.from(닭가슴살, 200.0);

        assertThat(item.getCalories()).isEqualTo(330.0);
        assertThat(item.getProtein()).isEqualTo(62.0);
        assertThat(item.getFat()).isEqualTo(7.2);
    }

    @Test
    void 섭취량이_0g이면_모든_영양소가_0이다() {
        MealItem item = MealItem.from(닭가슴살, 0.0);

        assertThat(item.getCalories()).isEqualTo(0.0);
        assertThat(item.getCarbs()).isEqualTo(0.0);
        assertThat(item.getProtein()).isEqualTo(0.0);
        assertThat(item.getFat()).isEqualTo(0.0);
        assertThat(item.getFiber()).isEqualTo(0.0);
    }
}
