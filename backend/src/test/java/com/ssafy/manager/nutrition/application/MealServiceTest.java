package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.domain.MealType;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    @Mock FoodRepository foodRepository;
    @Mock MealRepository mealRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MealService mealService;

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 6, 2, 12, 0);
    private static final String FOOD_CODE = "D000001";
    /** isWithin 하한(90%) 미만임을 의도한 비율 */
    private static final double BELOW_RANGE_RATIO = 0.8;

    @Test
    void 칼로리_합산이_목표에_처음_도달하면_Streak이_증가한다() {
        Food food = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(food));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(2000.0);

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.LUNCH, TODAY, List.of(new MealItemCommand(FOOD_CODE, 100.0))),
                NOON
        );

        verify(eventPublisher).publishEvent(new MealGoalAchievedEvent(MEMBER_ID, TODAY));
    }

    @Test
    void 이미_목표를_달성한_상태면_Streak이_증가하지_않는다() {
        Food food = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(food));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(2500.0);

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 2000.0);
        goal.recalculate(2000.0, 0, 0, 0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.DINNER, TODAY, List.of(new MealItemCommand(FOOD_CODE, 50.0))),
                NOON
        );

        verify(eventPublisher, never()).publishEvent(new MealGoalAchievedEvent(MEMBER_ID, TODAY));
    }

    @Test
    void 영양소_목표를_모두_달성하면_Streak이_증가한다() {
        Food food = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(food));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(2000.0);
        given(mealItemRepository.sumProteinByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(50.0);
        given(mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(250.0);
        given(mealItemRepository.sumFatByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(70.0);

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 2000.0, 50.0, 250.0, 70.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.LUNCH, TODAY, List.of(new MealItemCommand(FOOD_CODE, 100.0))),
                NOON
        );

        verify(eventPublisher).publishEvent(new MealGoalAchievedEvent(MEMBER_ID, TODAY));
    }

    @ParameterizedTest(name = "{0} 미달 시 Streak 미증가")
    @MethodSource("underAchievedNutrients")
    void 영양소_중_하나라도_미달되면_Streak이_증가하지_않는다(String label, double calories, double protein, double carbs, double fat) {
        Food food = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(food));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(calories);
        given(mealItemRepository.sumProteinByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(protein);
        given(mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(carbs);
        given(mealItemRepository.sumFatByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(fat);

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 2000.0, 50.0, 250.0, 70.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.LUNCH, TODAY, List.of(new MealItemCommand(FOOD_CODE, 100.0))),
                NOON
        );

        verify(eventPublisher, never()).publishEvent(new MealGoalAchievedEvent(MEMBER_ID, TODAY));
    }

    @Test
    void 새벽_4시_전_기록은_전날_DailyGoal에_적용된다() {
        LocalDate yesterday = TODAY.minusDays(1);
        LocalDateTime at3am = LocalDateTime.of(2026, 6, 2, 3, 0);

        Food food = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(food));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, yesterday)).willReturn(1800.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, yesterday)).willReturn(Optional.empty());

        mealService.record(
                new MealCommand(MEMBER_ID, MealType.SNACK, TODAY, List.of(new MealItemCommand(FOOD_CODE, 100.0))),
                at3am
        );

        verify(dailyGoalRepository).findByMemberIdAndDate(MEMBER_ID, yesterday);
        verify(mealItemRepository).sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, yesterday);
    }

    static Stream<Arguments> underAchievedNutrients() {
        return Stream.of(
                Arguments.of("칼로리",  2000.0 * BELOW_RANGE_RATIO, 50.0,                      250.0,                      70.0),
                Arguments.of("단백질",  2000.0,                     50.0 * BELOW_RANGE_RATIO,  250.0,                      70.0),
                Arguments.of("탄수화물", 2000.0,                     50.0,                      250.0 * BELOW_RANGE_RATIO,  70.0),
                Arguments.of("지방",    2000.0,                     50.0,                      250.0,                      70.0 * BELOW_RANGE_RATIO)
        );
    }
}
