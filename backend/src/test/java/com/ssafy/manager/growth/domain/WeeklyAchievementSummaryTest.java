package com.ssafy.manager.growth.domain;

import com.ssafy.manager.program.domain.DailyGoal;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeeklyAchievementSummaryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 9);    // 화요일
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 8);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 6, 14);

    @Test
    void DailyGoal_목록이_8개_이상이면_예외가_발생한다() {
        List<DailyGoal> eightGoals = List.of(
                DailyGoal.of(1L, MONDAY, 2000),
                DailyGoal.of(1L, MONDAY.plusDays(1), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(2), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(3), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(4), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(5), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(6), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(7), 2000)
        );

        assertThatThrownBy(() -> WeeklyAchievementSummary.of(TODAY, eightGoals))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void DailyGoal_목록이_7개_이하이면_정상_생성된다() {
        List<DailyGoal> sevenGoals = List.of(
                DailyGoal.of(1L, MONDAY, 2000),
                DailyGoal.of(1L, MONDAY.plusDays(1), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(2), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(3), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(4), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(5), 2000),
                DailyGoal.of(1L, MONDAY.plusDays(6), 2000)
        );

        assertThat(WeeklyAchievementSummary.of(TODAY, sevenGoals)).isNotNull();
    }

    @Test
    void today가_속한_주의_weekStart와_weekEnd를_반환한다() {
        WeeklyAchievementSummary summary = WeeklyAchievementSummary.of(TODAY, List.of());

        assertThat(summary.weekStart()).isEqualTo(MONDAY);
        assertThat(summary.weekEnd()).isEqualTo(SUNDAY);
    }

    @Test
    void 기록이_있는_날짜의_달성률을_반환한다() {
        DailyGoal goal = DailyGoal.of(1L, MONDAY, 2000);
        goal.recalculate(1500);

        WeeklyAchievementSummary summary = WeeklyAchievementSummary.of(TODAY, List.of(goal));

        assertThat(summary.achievementRateFor(MONDAY)).isEqualTo(1500.0 / 2000.0);
    }

    @Test
    void 기록이_없는_날짜는_달성률_0을_반환한다() {
        WeeklyAchievementSummary summary = WeeklyAchievementSummary.of(TODAY, List.of());

        assertThat(summary.achievementRateFor(MONDAY)).isEqualTo(0.0);
    }
}
