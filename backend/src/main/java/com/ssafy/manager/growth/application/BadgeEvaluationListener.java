package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.nutrition.application.MealRecordedEvent;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.routine.application.WorkoutLoggedEvent;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 사실 이벤트를 수신해 운동 계열 뱃지 조건을 평가하고 발행한다 (docs/adr/0001).
 *
 * <p>동기 {@link EventListener}이므로 발행자(routine)의 트랜잭션 안에서 실행된다.
 */
@Component
@RequiredArgsConstructor
public class BadgeEvaluationListener {

    private static final long ALL_RIGHT_THRESHOLD = 100;
    private static final long WEEKEND_WARRIOR_THRESHOLD = 10;

    private static final long VEGGIE_MANIA_THRESHOLD = 20;
    private static final double VEGGIE_MANIA_MIN_FIBER = 8.0;
    private static final long CHICKEN_BREAST_THRESHOLD = 30;
    private static final String CHICKEN_BREAST_KEYWORD = "닭가슴살";

    private final BadgeService badgeService;
    private final RoutineSessionRepository routineSessionRepository;
    private final MealRepository mealRepository;

    @EventListener
    public void on(WorkoutLoggedEvent event) {
        Long memberId = event.memberId();

        if (routineSessionRepository.countByMemberId(memberId) >= ALL_RIGHT_THRESHOLD) {
            badgeService.grant(memberId, Badge.ALL_RIGHT);
        }
        if (routineSessionRepository.countWeekendByMemberId(memberId) >= WEEKEND_WARRIOR_THRESHOLD) {
            badgeService.grant(memberId, Badge.WEEKEND_WARRIOR);
        }
    }

    @EventListener
    public void on(MealRecordedEvent event) {
        Long memberId = event.memberId();

        if (mealRepository.countFiberRichMealsByMemberId(memberId, VEGGIE_MANIA_MIN_FIBER)
                >= VEGGIE_MANIA_THRESHOLD) {
            badgeService.grant(memberId, Badge.VEGGIE_MANIA);
        }
        if (mealRepository.countMealsContainingFoodNameByMemberId(memberId, CHICKEN_BREAST_KEYWORD)
                >= CHICKEN_BREAST_THRESHOLD) {
            badgeService.grant(memberId, Badge.CHICKEN_BREAST_EVANGELIST);
        }
    }
}
