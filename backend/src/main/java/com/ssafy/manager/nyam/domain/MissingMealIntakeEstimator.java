package com.ssafy.manager.nyam.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

/**
 * 식사 기록이 전혀 없는 날의 섭취 칼로리를 추정한다.
 * 목표 칼로리의 ±20% 범위에서, (memberId, date) 시드 기반 결정론적 난수로 산출한다.
 * 같은 날짜를 재처리해도 항상 같은 값이 나온다.
 */
public final class MissingMealIntakeEstimator {

    private MissingMealIntakeEstimator() {
    }

    public static double estimate(int targetCalories, long memberId, LocalDate date) {
        Random random = new Random(Objects.hash(memberId, date));
        double factor = 0.8 + random.nextDouble() * 0.4; // [0.8, 1.2)
        return targetCalories * factor;
    }
}
