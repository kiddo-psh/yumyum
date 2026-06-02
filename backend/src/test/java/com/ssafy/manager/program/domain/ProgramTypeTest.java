package com.ssafy.manager.program.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramTypeTest {

    private static final int TDEE = 2664;

    @Test
    void DIET_adjust_결과는_TDEE에서_500을_차감한다() {
        assertThat(ProgramType.DIET.adjust(TDEE)).isEqualTo(2164);
    }

    @Test
    void DIET_adjust_결과는_반드시_TDEE보다_200_이상_낮다() {
        assertThat(ProgramType.DIET.adjust(TDEE)).isLessThanOrEqualTo(TDEE - 200);
    }

    @Test
    void MUSCLE_adjust_결과는_TDEE에_300을_추가한다() {
        assertThat(ProgramType.MUSCLE.adjust(TDEE)).isEqualTo(2964);
    }

    @Test
    void MUSCLE_adjust_결과는_반드시_TDEE보다_200_이상_높다() {
        assertThat(ProgramType.MUSCLE.adjust(TDEE)).isGreaterThanOrEqualTo(TDEE + 200);
    }

    @Test
    void HEALTH_DISEASE_adjust_결과는_TDEE와_같다() {
        assertThat(ProgramType.HEALTH.adjust(TDEE)).isEqualTo(TDEE);
        assertThat(ProgramType.DISEASE.adjust(TDEE)).isEqualTo(TDEE);
    }
}
