package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.CalorieBalanceResponse;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CalorieBalanceServiceTest {

    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock MealRepository mealRepository;
    @Mock RoutineSessionRepository routineSessionRepository;

    @InjectMocks CalorieBalanceService calorieBalanceService;

    private static final LocalDate DATE = LocalDate.of(2026, 6, 10);
    private static final Long MEMBER_ID = 1L;

    @Test
    void DailyGoal없으면_0으로_채운_응답_반환() {
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.empty());

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(18, 0));

        assertThat(resp.targetCalories()).isEqualTo(0);
        assertThat(resp.lastMealRecommendTrigger()).isFalse();
    }

    @Test
    void 운동_소모_칼로리가_잔여칼로리에_반영된다() {
        DailyGoal goal = makeDailyGoal(2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.of(goal));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(1500.0);
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(300L);
        given(mealRepository.countByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(2);

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(16, 0));

        // remaining = 2000 + 300 - 1500 = 800
        assertThat(resp.remainingCalories()).isEqualTo(800.0);
        assertThat(resp.burnedCalories()).isEqualTo(300);
    }

    @Test
    void 트리거_조건_충족시_lastMealRecommendTrigger_true() {
        // target=2000, intake=1500, burned=0, mealCount=3
        // oneMealTarget = 2000/(3+1) = 500
        // remaining = 2000 - 1500 = 500 → within 500 * [0.8, 1.2] = [400, 600] → true
        DailyGoal goal = makeDailyGoal(2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.of(goal));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(1500.0);
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(0L);
        given(mealRepository.countByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(3);

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(18, 0));

        assertThat(resp.lastMealRecommendTrigger()).isTrue();
    }

    @Test
    void 오후5시_미만이면_트리거_false() {
        DailyGoal goal = makeDailyGoal(2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(Optional.of(goal));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(1333.0);
        given(routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(MEMBER_ID, DATE)).willReturn(0L);
        given(mealRepository.countByMemberIdAndEffectiveDate(MEMBER_ID, DATE)).willReturn(3);

        CalorieBalanceResponse resp = calorieBalanceService.getBalance(MEMBER_ID, DATE, LocalTime.of(14, 0));

        assertThat(resp.lastMealRecommendTrigger()).isFalse();
    }

    private DailyGoal makeDailyGoal(double targetValue) {
        return DailyGoal.of(MEMBER_ID, DATE, targetValue);
    }
}
