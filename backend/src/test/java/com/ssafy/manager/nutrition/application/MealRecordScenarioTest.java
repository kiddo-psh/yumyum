package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.growth.application.StreakService;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.domain.MealType;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Meal 기록 → DailyGoal achievedCalories 누적 → 목표 초과 시 achieved = true + Streak 증가
 *
 * TODO: 현재는 Mock 기반 단위 테스트. 실제 DB 레이어와 연동하는 통합 테스트로 교체 필요.
 */
@ExtendWith(MockitoExtension.class)
class MealRecordScenarioTest {

    @Mock FoodRepository foodRepository;
    @Mock MealRepository mealRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock StreakService streakService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MealService mealService;

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 6, 2, 12, 0);
    private static final String FOOD_CODE = "D000001";

    @Test
    void 식사_기록_후_목표_칼로리_달성_시_DailyGoal이_achieved되고_Streak이_증가한다() {
        Food chicken = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(chicken));

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 1650.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(1650.0);

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.LUNCH, TODAY, List.of(new MealItemCommand(FOOD_CODE, 1000.0))),
                NOON
        );

        assertThat(goal.isAchieved()).isTrue();
        verify(streakService).increment(MEMBER_ID, TODAY);
    }

    @Test
    void 식사_기록_후_목표_칼로리_미달_시_DailyGoal이_미달성이고_Streak은_변하지_않는다() {
        Food chicken = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(chicken));

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(500.0);

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.BREAKFAST, TODAY, List.of(new MealItemCommand(FOOD_CODE, 100.0))),
                NOON
        );

        assertThat(goal.isAchieved()).isFalse();
        assertThat(goal.getAchievedValue()).isEqualTo(500.0);
        org.mockito.Mockito.verifyNoInteractions(streakService);
    }
}
