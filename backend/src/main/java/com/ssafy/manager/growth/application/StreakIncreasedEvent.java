package com.ssafy.manager.growth.application;

/**
 * 스트릭이 실제로 증가했을 때만 발행되는 결과 이벤트 (docs/adr/0001 체이닝).
 *
 * <p>연속 달성 계열 뱃지 평가는 이 이벤트를 구독한다 — 스트릭 값이 이미 반영된 상태를
 * 보장받으며, 같은 날 두 번째 활동(no-op 증가)에서는 발행되지 않아 하루 1회만 평가된다.
 */
public record StreakIncreasedEvent(Long memberId, int currentStreak) {
}
