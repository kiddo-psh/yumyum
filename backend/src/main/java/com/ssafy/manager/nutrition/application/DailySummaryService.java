package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.member.domain.OnboardingRequiredException;
import com.ssafy.manager.nutrition.domain.MealItem;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.presentation.dto.DailySummaryResponse;
import com.ssafy.manager.program.application.DailyGoalCreationService;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailyGoalCreationService dailyGoalCreationService;
    private final DailyGoalRepository dailyGoalRepository;
    private final MemberStatsRepository memberStatsRepository;
    private final MealItemRepository mealItemRepository;

    @Transactional(readOnly = true)
    public DailySummaryResponse getSummary(Long memberId, LocalDate date) {
        dailyGoalCreationService.ensureGoalExists(memberId, date);
        DailyGoal goal = dailyGoalRepository.findByMemberIdAndDate(memberId, date)
                .orElseThrow(() -> new NoSuchElementException("해당 날짜의 DailyGoal이 없습니다."));

        var stats = memberStatsRepository.findByMemberId(memberId)
                .orElseThrow(() -> new OnboardingRequiredException("온보딩이 필요합니다."));

        List<MealItem> items = mealItemRepository.findAllByMemberIdAndEffectiveDate(memberId, date);
        double totalCarbs   = items.stream().mapToDouble(MealItem::getCarbs).sum();
        double totalProtein = items.stream().mapToDouble(MealItem::getProtein).sum();
        double totalFat     = items.stream().mapToDouble(MealItem::getFat).sum();

        return new DailySummaryResponse(
                (int) goal.getTargetValue(),
                goal.getAchievedValue(),
                goal.isAchieved(),
                stats.getCurrentStreak().count(),
                stats.getMaxStreak().count(),
                totalCarbs,
                totalProtein,
                totalFat
        );
    }
}
