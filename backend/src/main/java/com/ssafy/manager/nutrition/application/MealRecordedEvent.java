package com.ssafy.manager.nutrition.application;

/**
 * 식사가 기록되었다는 사실 이벤트.
 *
 * <p>발생원(nutrition)은 "식사가 기록되었다"만 알린다. 식단 계열 뱃지 평가 등
 * growth의 반응은 이 이벤트를 구독하는 리스너가 담당한다 (docs/adr/0001).
 *
 * <p>스트릭/일일 목표 달성과는 별개의 신호다 — 목표 달성 여부와 무관하게
 * 모든 식사 기록마다 발행되어, 누적 식사 수를 세는 뱃지의 트리거가 된다.
 */
public record MealRecordedEvent(Long memberId, Long mealId) {
}
