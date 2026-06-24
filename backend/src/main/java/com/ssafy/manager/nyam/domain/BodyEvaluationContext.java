package com.ssafy.manager.nyam.domain;

/**
 * 체형 카테고리 판정에 필요한 입력을 모은 값 객체.
 * 데이터 수집(루틴·세션·체형상태·회원)은 서비스가 담당하고,
 * 룰은 이 컨텍스트만 보고 판정한다(순수).
 */
public record BodyEvaluationContext(
        double virtualWeightKg,
        double heightCm,
        int daysPerWeekTarget,
        int recentWeekWorkoutDays,
        int priorWeekWorkoutDays
) {
}
