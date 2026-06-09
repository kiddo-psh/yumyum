package com.ssafy.manager.growth.domain;

import com.ssafy.manager.program.domain.DailyGoal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeeklyAchievementSummary {

    private final LocalDate weekStart;
    private final LocalDate weekEnd;
    private final Map<LocalDate, DailyGoal> goalsByDate;

    private WeeklyAchievementSummary(LocalDate today, List<DailyGoal> goals) {
        this.weekStart = today.with(DayOfWeek.MONDAY);
        this.weekEnd = weekStart.plusDays(6);
        this.goalsByDate = goals.stream()
                .collect(Collectors.toMap(DailyGoal::getDate, g -> g));
    }

    public static WeeklyAchievementSummary of(LocalDate today, List<DailyGoal> goals) {
        if (goals.size() > 7) {
            throw new IllegalArgumentException("주간 DailyGoal 목록은 7개를 초과할 수 없습니다.");
        }
        return new WeeklyAchievementSummary(today, goals);
    }

    public LocalDate weekStart() {
        return weekStart;
    }

    public LocalDate weekEnd() {
        return weekEnd;
    }

    public double achievementRateFor(LocalDate date) {
        DailyGoal goal = goalsByDate.get(date);
        if (goal == null) return 0.0;
        return goal.getAchievedValue() / goal.getTargetValue();
    }
}
