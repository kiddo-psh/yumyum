package com.ssafy.manager.global.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 새벽 04:00 이전 기록은 전날 기준(Effective Date)으로 처리한다.
 * DailyGoal·Meal 등 "하루 단위" 데이터를 다루는 모든 곳에서 이 기준을 공유해야
 * 날짜 경계(자정~04:00)에서 데이터가 어긋나지 않는다.
 */
public final class EffectiveDateResolver {

    private static final int CUTOFF_HOUR = 4;

    private EffectiveDateResolver() {
    }

    public static LocalDate resolve(LocalDateTime at) {
        return at.getHour() < CUTOFF_HOUR
                ? at.toLocalDate().minusDays(1)
                : at.toLocalDate();
    }

    public static LocalDate today() {
        return resolve(LocalDateTime.now());
    }
}
