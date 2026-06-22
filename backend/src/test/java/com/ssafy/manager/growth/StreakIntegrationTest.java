package com.ssafy.manager.growth;

import com.ssafy.manager.growth.application.StreakService;
import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.domain.Streak;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.nutrition.application.MealCommand;
import com.ssafy.manager.nutrition.application.MealItemCommand;
import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.domain.MealType;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.routine.application.RoutineAiAdjustService;
import com.ssafy.manager.routine.application.RoutineSessionService;
import com.ssafy.manager.routine.application.SessionSetInput;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class StreakIntegrationTest {

    @Mock RoutineRepository routineRepository;
    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock SessionSetRepository sessionSetRepository;
    @Mock MemberStatsRepository memberStatsRepository;
    @Mock FoodRepository foodRepository;
    @Mock MealRepository mealRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock RoutineAiAdjustService routineAiAdjustService;

    RoutineSessionService routineSessionService;
    MealService mealService;

    private final LocalDate YESTERDAY = LocalDate.of(2026, 6, 21);
    private final LocalDate TODAY = YESTERDAY.plusDays(1);
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 6, 22, 12, 0);
    private final long MEMBER_ID = 1L;
    private final long ROUTINE_ID = 1L;
    private static final String FOOD_CODE = "D000001";

    @BeforeEach
    void setUp() {
        routineSessionService = new RoutineSessionService(
                routineRepository,
                routineSessionRepository,
                sessionSetRepository,
                routineAiAdjustService,
                streakService()
        );

        mealService = new MealService(
                foodRepository,
                mealRepository,
                mealItemRepository,
                dailyGoalRepository,
                streakService()
        );
    }

    @Test
    void 식단_기록과_운동_기록이_모두_발생해도_Streak은_1번_증가한다() {
        MemberStats memberStats = new MemberStats(Streak.of(10), Streak.of(10), YESTERDAY);
        List<SessionSetInput> sessionRecords = getSessionRecords();

        beforeRecordSession(memberStats);
        beforeRecordMeal();

        routineSessionService.recordSession(MEMBER_ID, ROUTINE_ID, TODAY, 0, sessionRecords);
        mealService.record(
                new MealCommand(MEMBER_ID, MealType.DINNER, TODAY, List.of(new MealItemCommand(FOOD_CODE, 50.0))),
                NOON
        );

        assertThat(memberStats.getCurrentStreak().count()).isEqualTo(11);
        assertThat(memberStats.getLastAchievedDate()).isEqualTo(TODAY);
    }

    private void beforeRecordMeal() {
        Food food = new Food(FOOD_CODE, "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0);
        given(foodRepository.findByCode(FOOD_CODE)).willReturn(Optional.of(food));

        DailyGoal goal = DailyGoal.of(MEMBER_ID, TODAY, 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(MEMBER_ID, TODAY)).willReturn(Optional.of(goal));

        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(2000.0);
        given(mealItemRepository.sumProteinByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(50.0);
        given(mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(250.0);
        given(mealItemRepository.sumFatByMemberIdAndEffectiveDate(MEMBER_ID, TODAY)).willReturn(70.0);
    }

    private void beforeRecordSession(MemberStats memberStats) {
        given(routineRepository.existsById(1L)).willReturn(true);
        given(memberStatsRepository.findByMemberId(1L)).willReturn(Optional.of(memberStats));
    }

    private List<SessionSetInput> getSessionRecords() {
        return List.of(
                new SessionSetInput(10L, "벤치프레스", 1, 8, 60.0, true)
        );
    }

    private StreakService streakService() {
        return new StreakService(memberStatsRepository);
    }

}
