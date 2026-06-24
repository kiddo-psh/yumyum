package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.growth.application.StreakService;
import com.ssafy.manager.nutrition.domain.Food;
import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.domain.FoodRepository;
import com.ssafy.manager.nutrition.domain.MealType;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Meal record(MealCommand command, LocalDateTime recordedAt) {
        LocalDate effectiveDate = effectiveDateOf(recordedAt);
        MealType resolvedType = command.type() != null ? command.type() : inferMealType(recordedAt);

        Meal meal = new Meal(command.memberId(), resolvedType, command.date(), effectiveDate);
        for (MealItemCommand itemCmd : command.items()) {
            Food food = foodRepository.findByCode(itemCmd.foodCode()).orElseThrow();
            meal.addItem(food, itemCmd.amountGrams());
        }
        mealRepository.save(meal);

        double totalCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(command.memberId(), effectiveDate);

        dailyGoalRepository.findByMemberIdAndDate(command.memberId(), effectiveDate)
                .ifPresent(goal -> updateGoalAndStreak(goal, totalCalories, command.memberId(), effectiveDate));

        eventPublisher.publishEvent(new MealRecordedEvent(command.memberId(), meal.getId()));
        return meal;
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

    @Transactional
    public Meal addItem(Long mealId, Long memberId, MealItemCommand itemCmd) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow();
        if (!meal.getMemberId().equals(memberId)) {
            throw new ForbiddenException("해당 식사에 접근 권한이 없습니다.");
        }
        Food food = foodRepository.findByCode(itemCmd.foodCode()).orElseThrow();
        meal.addItem(food, itemCmd.amountGrams());

        LocalDate effectiveDate = meal.getEffectiveDate();
        double totalCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(memberId, effectiveDate);
        dailyGoalRepository.findByMemberIdAndDate(memberId, effectiveDate)
                .ifPresent(goal -> updateGoalAndStreak(goal, totalCalories, memberId, effectiveDate));

        return meal;
    }

    @Transactional
    public void delete(Long mealId, Long memberId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow();
        if (!meal.getMemberId().equals(memberId)) {
            throw new ForbiddenException("해당 식사에 접근 권한이 없습니다.");
        }
        mealRepository.delete(meal);
    }

    @Transactional
    public Meal recordFromPhoto(PhotoMealCommand command, LocalDateTime recordedAt) {
        LocalDate effectiveDate = effectiveDateOf(recordedAt);
        MealType resolvedType = command.mealType() != null ? command.mealType() : inferMealType(recordedAt);

        Meal meal = new Meal(command.memberId(), resolvedType, recordedAt.toLocalDate(), effectiveDate);
        for (PhotoMealItemCommand item : command.items()) {
            meal.addAiItem(item.name(), item.estimatedGrams(),
                    item.kcal(), item.proteinG(), item.carbG(), item.fatG());
        }
        mealRepository.save(meal);

        double totalCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(
                command.memberId(), effectiveDate);
        dailyGoalRepository.findByMemberIdAndDate(command.memberId(), effectiveDate)
                .ifPresent(goal -> updateGoalAndStreak(goal, totalCalories, command.memberId(), effectiveDate));

        eventPublisher.publishEvent(new MealRecordedEvent(command.memberId(), meal.getId()));
        return meal;
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

    private MealType inferMealType(LocalDateTime recordedAt) {
        int hour = recordedAt.getHour();
        if (hour >= 4 && hour < 10) return MealType.BREAKFAST;
        if (hour < 15) return MealType.LUNCH;
        if (hour < 20) return MealType.DINNER;
        return MealType.SNACK;
    }
}
