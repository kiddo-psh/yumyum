package com.ssafy.manager.routine.application;

import java.time.LocalDate;

/**
 * 운동 세션이 기록되었다는 사실 이벤트.
 *
 * <p>발생원(routine)은 "운동이 기록되었다"만 알린다. 스트릭 증가·뱃지 발행 등
 * growth의 반응은 이 이벤트를 구독하는 리스너가 담당한다 (docs/adr/0001).
 */
public record WorkoutLoggedEvent(Long memberId, LocalDate sessionDate) {
}
