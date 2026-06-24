package com.ssafy.manager.growth.application;

/**
 * 스트릭 증가 시도의 결과.
 *
 * @param increased     이번 호출로 스트릭이 실제 증가했는지 (같은 날 재시도면 false)
 * @param currentStreak 갱신 후 현재 연속 달성일
 */
public record StreakUpdate(boolean increased, int currentStreak) {
}
