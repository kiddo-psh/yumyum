package com.ssafy.manager.nyam.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DailyBalanceCalculatorTest {

    private final LocalDate date = LocalDate.of(2026, 6, 15);

    @Test
    void 식사_기록이_있으면_실제_섭취로_밸런스를_계산한다() {
        // intake 2500 - (TDEE 2000 + 운동 300) = +200
        double balance = DailyBalanceCalculator.calculate(
                2000, 2500, 2, 300, 2000, 1L, date);

        assertThat(balance).isEqualTo(200.0);
    }

    @Test
    void 섭취가_총소비보다_적으면_결손_밸런스가_나온다() {
        // intake 1500 - (TDEE 2000 + 운동 0) = -500
        double balance = DailyBalanceCalculator.calculate(
                2000, 1500, 1, 0, 2000, 1L, date);

        assertThat(balance).isEqualTo(-500.0);
    }

    @Test
    void 식사_기록이_없으면_목표_80에서_120퍼센트_추정치로_계산한다() {
        double balance = DailyBalanceCalculator.calculate(
                2000, 0, 0, 0, 2000, 1L, date);

        // intake ∈ [1600, 2400], 소비 2000 → balance ∈ [-400, 400]
        assertThat(balance).isBetween(-400.0, 400.0);
    }
}
