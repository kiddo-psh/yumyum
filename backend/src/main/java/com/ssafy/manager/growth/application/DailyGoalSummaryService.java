package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.DailyProgress;
import com.ssafy.manager.growth.domain.WeeklyAchievementSummary;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyGoalSummaryService {

    private final DailyGoalRepository dailyGoalRepository;

    @Transactional(readOnly = true)
    public WeeklyAchievementSummary weeklyCalendar(Long memberId, LocalDate today, int weekOffset) {
        LocalDate target = today.plusWeeks(weekOffset);
        LocalDate weekStart = target.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyGoal> goals = dailyGoalRepository
                .findAllByMemberIdAndDateBetween(memberId, weekStart, weekEnd);

        return WeeklyAchievementSummary.of(target, goals);
    }

    @Transactional(readOnly = true)
    public DailyProgress todayProgress(Long memberId, LocalDate today) {
        return dailyGoalRepository.findByMemberIdAndDate(memberId, today)
                .map(DailyProgress::from)
                .orElse(DailyProgress.empty());
    }
}
