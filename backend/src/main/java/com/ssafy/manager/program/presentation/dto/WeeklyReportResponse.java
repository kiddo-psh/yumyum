package com.ssafy.manager.program.presentation.dto;

import com.ssafy.manager.program.domain.WeeklyReport;

public record WeeklyReportResponse(
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
    public static WeeklyReportResponse from(WeeklyReport report) {
        return new WeeklyReportResponse(
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
