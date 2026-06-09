package com.ssafy.manager.growth.application;

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
    public WeeklyAchievementSummary weeklyCalendar(Long memberId, LocalDate today) {
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyGoal> goals = dailyGoalRepository
                .findAllByMemberIdAndDateBetween(memberId, weekStart, weekEnd);

        return WeeklyAchievementSummary.of(today, goals);
    }
}
