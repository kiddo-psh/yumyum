package com.ssafy.manager.program.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportTest {

    @Test
    void fill_호출_후_모든_필드가_채워진다() {
        WeeklyReport report = new WeeklyReport(1L, 1);

        report.fill("ai코멘트", "영양요약", "운동요약", "목표요약", 87.5, 5, -0.3);

        assertThat(report.getContent()).isEqualTo("ai코멘트");
        assertThat(report.getNutritionSummary()).isEqualTo("영양요약");
        assertThat(report.getExerciseSummary()).isEqualTo("운동요약");
        assertThat(report.getGoalSummary()).isEqualTo("목표요약");
        assertThat(report.getAvgCalorieRate()).isEqualTo(87.5);
        assertThat(report.getAchievementDays()).isEqualTo(5);
        assertThat(report.getWeightTrend()).isEqualTo(-0.3);
    }

    @Test
    void fill_전_content는_null이다() {
        WeeklyReport report = new WeeklyReport(1L, 1);
        assertThat(report.getContent()).isNull();
    }

    @Test
    void weightTrend는_null_허용된다() {
        WeeklyReport report = new WeeklyReport(1L, 1);
        report.fill("comment", "n", "e", "g", 80.0, 4, null);
        assertThat(report.getWeightTrend()).isNull();
    }
}
