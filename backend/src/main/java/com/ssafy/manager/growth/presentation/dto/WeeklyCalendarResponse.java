package com.ssafy.manager.growth.presentation.dto;

import com.ssafy.manager.growth.domain.DailyAchievementStatus;
import com.ssafy.manager.growth.domain.WeeklyAchievementSummary;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record WeeklyCalendarResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<DayEntry> days
) {
    public record DayEntry(
            LocalDate date,
            double achievementRate,
            DailyAchievementStatus status
    ) {}

    public static WeeklyCalendarResponse from(WeeklyAchievementSummary summary) {
        List<DayEntry> days = new ArrayList<>();
        for (LocalDate date = summary.weekStart(); !date.isAfter(summary.weekEnd()); date = date.plusDays(1)) {
            double rate = Math.min(summary.achievementRateFor(date), 1.0);
            days.add(new DayEntry(date, rate, DailyAchievementStatus.of(rate)));
        }
        return new WeeklyCalendarResponse(summary.weekStart(), summary.weekEnd(), days);
    }
}
