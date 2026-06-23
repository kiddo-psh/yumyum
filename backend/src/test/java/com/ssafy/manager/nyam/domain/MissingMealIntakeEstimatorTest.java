package com.ssafy.manager.nyam.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MissingMealIntakeEstimatorTest {

    private final LocalDate date = LocalDate.of(2026, 6, 15);

    @Test
    void 추정치는_목표칼로리의_80에서_120퍼센트_범위_안이다() {
        int target = 2000;

        double estimate = MissingMealIntakeEstimator.estimate(target, 1L, date);

        assertThat(estimate).isBetween(target * 0.8, target * 1.2);
    }

    @Test
    void 같은_회원_같은_날짜는_항상_같은_추정치를_낸다() {
        double first = MissingMealIntakeEstimator.estimate(2000, 1L, date);
        double second = MissingMealIntakeEstimator.estimate(2000, 1L, date);

        assertThat(first).isEqualTo(second);
    }
}
