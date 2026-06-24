package com.ssafy.manager.growth.presentation.dto;

import com.ssafy.manager.growth.application.StreakChangeHolder;

/**
 * 기록 API piggyback 응답의 {@code streak} 필드 (docs/adr/0002).
 *
 * @param increased 이번 요청으로 스트릭이 증가했는지 (프론트의 "스트릭 갱신 화면" 노출 조건)
 * @param current   현재 연속 달성일. 이번 요청에 스트릭 활동이 없었으면 null
 */
public record StreakChangeResponse(boolean increased, Integer current) {

    public static StreakChangeResponse from(StreakChangeHolder holder) {
        return new StreakChangeResponse(holder.isIncreased(), holder.getCurrentStreak());
    }
}
