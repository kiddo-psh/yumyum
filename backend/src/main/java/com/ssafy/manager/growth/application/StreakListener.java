package com.ssafy.manager.growth.application;

import com.ssafy.manager.nutrition.application.MealGoalAchievedEvent;
import com.ssafy.manager.routine.application.WorkoutLoggedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 식단 목표 달성·운동 기록 사실 이벤트를 수신해 스트릭을 갱신한다 (docs/adr/0001).
 *
 * <p>실제로 증가했을 때만 {@link StreakIncreasedEvent}를 발행해 연속 달성 뱃지 평가로
 * 체이닝하고, 변화는 {@link StreakChangeHolder}에 적재해 piggyback 응답에 싣는다.
 * 동기 {@link EventListener}이므로 발행자의 트랜잭션·요청 스코프 안에서 실행된다.
 */
@Component
@RequiredArgsConstructor
public class StreakListener {

    private final StreakService streakService;
    private final StreakChangeHolder streakChangeHolder;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void on(MealGoalAchievedEvent event) {
        applyStreak(event.memberId(), event.date());
    }

    @EventListener
    public void on(WorkoutLoggedEvent event) {
        applyStreak(event.memberId(), event.sessionDate());
    }

    private void applyStreak(Long memberId, LocalDate date) {
        StreakUpdate update = streakService.increment(memberId, date);
        streakChangeHolder.record(update.increased(), update.currentStreak());
        if (update.increased()) {
            eventPublisher.publishEvent(new StreakIncreasedEvent(memberId, update.currentStreak()));
        }
    }
}
