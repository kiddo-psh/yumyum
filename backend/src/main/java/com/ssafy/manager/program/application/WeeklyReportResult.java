package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.WeeklyReport;

public record WeeklyReportResult(
        Long id,
        int weekNumber,
        String content,
        String nutritionSummary,
        String exerciseSummary,
        String goalSummary,
        double avgCalorieRate,
        int achievementDays,
        Double weightTrend
) {
    public static WeeklyReportResult from(WeeklyReport report) {
        return new WeeklyReportResult(
                report.getId(),
                report.getWeekNumber(),
                report.getContent(),
                report.getNutritionSummary(),
                report.getExerciseSummary(),
                report.getGoalSummary(),
                report.getAvgCalorieRate(),
                report.getAchievementDays(),
                report.getWeightTrend()
        );
    }
}
