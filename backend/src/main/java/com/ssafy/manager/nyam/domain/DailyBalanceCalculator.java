package com.ssafy.manager.nyam.domain;

import java.time.LocalDate;

/**
 * 하루치 칼로리 밸런스(잉여 +, 결손 −)를 계산한다.
 * baseline은 유지 칼로리(TDEE)이며, 운동 소모를 더한 총 소비 대비 섭취를 비교한다.
 * 식사 기록이 없는 날은 목표 칼로리의 ±20% 결정론적 추정치로 대체한다.
 */
public final class DailyBalanceCalculator {

    private DailyBalanceCalculator() {
    }

    public static double calculate(int targetCalories, double achievedCalories, int mealCount,
                                   double burnedCalories, double tdee,
                                   long memberId, LocalDate date) {
        double intake = mealCount > 0
                ? achievedCalories
                : MissingMealIntakeEstimator.estimate(targetCalories, memberId, date);
        return intake - (tdee + burnedCalories);
    }
}
