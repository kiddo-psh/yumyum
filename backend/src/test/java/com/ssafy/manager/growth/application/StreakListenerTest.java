package com.ssafy.manager.growth.application;

import com.ssafy.manager.nutrition.application.MealGoalAchievedEvent;
import com.ssafy.manager.routine.application.WorkoutLoggedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StreakListenerTest {

    @Mock StreakService streakService;
    @Mock StreakChangeHolder streakChangeHolder;
    @Mock ApplicationEventPublisher eventPublisher;

    private static final long MEMBER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 6, 22);

    private StreakListener listener() {
        return new StreakListener(streakService, streakChangeHolder, eventPublisher);
    }

    @Test
    void 식단_목표_달성시_스트릭이_증가하면_StreakIncreasedEvent를_발행한다() {
        given(streakService.increment(MEMBER_ID, DATE)).willReturn(new StreakUpdate(true, 7));

        listener().on(new MealGoalAchievedEvent(MEMBER_ID, DATE));

        verify(streakChangeHolder).record(true, 7);
        verify(eventPublisher).publishEvent(new StreakIncreasedEvent(MEMBER_ID, 7));
    }

    @Test
    void 운동_기록시_스트릭이_증가하면_StreakIncreasedEvent를_발행한다() {
        given(streakService.increment(MEMBER_ID, DATE)).willReturn(new StreakUpdate(true, 3));

        listener().on(new WorkoutLoggedEvent(MEMBER_ID, DATE));

        verify(eventPublisher).publishEvent(new StreakIncreasedEvent(MEMBER_ID, 3));
    }

    @Test
    void 스트릭이_증가하지_않으면_StreakIncreasedEvent를_발행하지_않는다() {
        given(streakService.increment(MEMBER_ID, DATE)).willReturn(new StreakUpdate(false, 7));

        listener().on(new MealGoalAchievedEvent(MEMBER_ID, DATE));

        verify(streakChangeHolder).record(false, 7);
        verify(eventPublisher, never()).publishEvent(any(StreakIncreasedEvent.class));
    }

    @Test
    void 같은_날_운동과_식단이_모두_발생해도_StreakIncreasedEvent는_한_번만_발행된다() {
        // 첫 활동(운동)은 증가, 두 번째 활동(식단)은 같은 날이라 no-op
        given(streakService.increment(MEMBER_ID, DATE))
                .willReturn(new StreakUpdate(true, 11))
                .willReturn(new StreakUpdate(false, 11));

        StreakListener listener = listener();
        listener.on(new WorkoutLoggedEvent(MEMBER_ID, DATE));
        listener.on(new MealGoalAchievedEvent(MEMBER_ID, DATE));

        verify(eventPublisher).publishEvent(new StreakIncreasedEvent(MEMBER_ID, 11));
        verify(eventPublisher, times(1)).publishEvent(any(StreakIncreasedEvent.class)); // exactly one fire
    }
}
