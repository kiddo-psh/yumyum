package com.ssafy.manager.growth.application;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 한 요청 동안의 스트릭 변화를 담는 홀더 (docs/adr/0002).
 *
 * <p>{@link StreakListener}가 동기 이벤트 처리 중 여기에 결과를 적재하고, 컨트롤러가
 * 서비스 호출 후 읽어 piggyback 응답의 {@code streak} 필드를 조립한다.
 *
 * <p>같은 날 식단·운동이 모두 발생하면 StreakListener가 두 번 호출되지만, {@code increased}는
 * 한 번이라도 증가하면 true로 유지된다(두 번째 no-op이 덮어쓰지 않음).
 */
@Component
@RequestScope
public class StreakChangeHolder {

    private boolean increased = false;
    private Integer currentStreak = null;

    public void record(boolean increased, int currentStreak) {
        this.increased = this.increased || increased;
        this.currentStreak = currentStreak;
    }

    public boolean isIncreased() {
        return increased;
    }

    /** 이번 요청에서 스트릭 활동이 전혀 없었으면 null. */
    public Integer getCurrentStreak() {
        return currentStreak;
    }
}
