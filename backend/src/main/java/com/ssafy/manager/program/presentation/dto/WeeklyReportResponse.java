package com.ssafy.manager.program.presentation.dto;

import com.ssafy.manager.program.application.WeeklyReportResult;

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
    public static WeeklyReportResponse from(WeeklyReportResult result) {
        return new WeeklyReportResponse(
                result.id(),
                result.weekNumber(),
                result.content(),
                result.nutritionSummary(),
                result.exerciseSummary(),
                result.goalSummary(),
                result.avgCalorieRate(),
                result.achievementDays(),
                result.weightTrend()
        );
    }
}
