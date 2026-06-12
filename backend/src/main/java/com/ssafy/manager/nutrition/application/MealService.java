package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.growth.application.StreakService;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.infrastructure.persistence.FoodRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealService {

    private final FoodRepository foodRepository;
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final StreakService streakService;

    @Transactional
    public Long record(MealCommand command, LocalDateTime recordedAt) {
        LocalDate effectiveDate = effectiveDateOf(recordedAt);

        Meal meal = new Meal(command.memberId(), command.type(), command.date(), effectiveDate);
        for (MealItemCommand itemCmd : command.items()) {
            Food food = foodRepository.findById(itemCmd.foodId()).orElseThrow();
            meal.addItem(food, itemCmd.amountGrams());
        }
        mealRepository.save(meal);

        double totalCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(command.memberId(), effectiveDate);

        dailyGoalRepository.findByMemberIdAndDate(command.memberId(), effectiveDate)
                .ifPresent(goal -> updateGoalAndStreak(goal, totalCalories, command.memberId(), effectiveDate));

        return meal.getId();
    }

    private void updateGoalAndStreak(DailyGoal goal, double totalCalories, Long memberId, LocalDate effectiveDate) {
        boolean wasAchieved = goal.isAchieved();
        double totalProtein = mealItemRepository.sumProteinByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalCarbs   = mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalFat     = mealItemRepository.sumFatByMemberIdAndEffectiveDate(memberId, effectiveDate);

        goal.recalculate(totalCalories, totalProtein, totalCarbs, totalFat);
        if (!wasAchieved && goal.isAchieved()) {
            streakService.increment(memberId, effectiveDate);
        }
    }

    @Transactional(readOnly = true)
    public List<Meal> listByDate(Long memberId, LocalDate date) {
        return mealRepository.findAllByMemberIdAndDate(memberId, date);
    }

    private LocalDate effectiveDateOf(LocalDateTime recordedAt) {
        return recordedAt.getHour() < 4
                ? recordedAt.toLocalDate().minusDays(1)
                : recordedAt.toLocalDate();
    }
}
