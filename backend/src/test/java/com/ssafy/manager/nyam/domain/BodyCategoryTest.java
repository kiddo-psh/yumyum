package com.ssafy.manager.nyam.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyCategoryTest {

    @Test
    void BMI_18_5_미만은_저체중() {
        assertThat(BodyCategory.fromBmi(18.4)).isEqualTo(BodyCategory.UNDERWEIGHT);
    }

    @Test
    void BMI_18_5에서_23_미만은_정상() {
        assertThat(BodyCategory.fromBmi(18.5)).isEqualTo(BodyCategory.NORMAL);
        assertThat(BodyCategory.fromBmi(22.9)).isEqualTo(BodyCategory.NORMAL);
    }

    @Test
    void BMI_23에서_25_미만은_과체중() {
        assertThat(BodyCategory.fromBmi(23)).isEqualTo(BodyCategory.OVERWEIGHT);
        assertThat(BodyCategory.fromBmi(24.9)).isEqualTo(BodyCategory.OVERWEIGHT);
    }

    @Test
    void BMI_25_이상은_비만() {
        assertThat(BodyCategory.fromBmi(25)).isEqualTo(BodyCategory.OBESE);
    }
}
