package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.CalorieBalanceResponse;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CalorieBalanceService {

    private final DailyGoalRepository dailyGoalRepository;
    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final RoutineSessionRepository routineSessionRepository;

    @Transactional(readOnly = true)
    public CalorieBalanceResponse getBalance(Long memberId, LocalDate date, LocalTime currentTime) {
        DailyGoal goal = dailyGoalRepository.findByMemberIdAndDate(memberId, date).orElse(null);
        if (goal == null) {
            return new CalorieBalanceResponse(0, 0.0, 0, 0.0, 0, false);
        }

        double intakeCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(memberId, date);
        int burnedCalories = (int) routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(memberId, date);
        int mealCount = mealRepository.countByMemberIdAndEffectiveDate(memberId, date);

        int targetCalories = (int) Math.round(goal.getTargetValue());
        double remainingCalories = targetCalories + burnedCalories - intakeCalories;

        boolean trigger = false;
        if (currentTime.getHour() >= 17 && mealCount >= 1) {
            double oneMealTarget = (double) targetCalories / mealCount;
            trigger = remainingCalories >= oneMealTarget * 0.8
                   && remainingCalories <= oneMealTarget * 1.2;
        }

        return new CalorieBalanceResponse(
                targetCalories, intakeCalories, burnedCalories,
                remainingCalories, mealCount, trigger
        );
    }
}
