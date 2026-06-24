package com.ssafy.manager.nutrition.application;

import java.time.LocalDate;

/**
 * 그날의 DailyGoal(식단 목표)을 처음 달성했다는 사실 이벤트.
 *
 * <p>매 식사를 알리는 {@link MealRecordedEvent}와 구분된다 — 이 이벤트는 목표를
 * 최초로 달성한 순간에만 발행되며, growth의 StreakListener가 구독해 스트릭을 올린다
 * (docs/adr/0001).
 */
public record MealGoalAchievedEvent(Long memberId, LocalDate date) {
}
