package com.ssafy.manager.growth.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MemberStatsTest {

    private MemberStats stats;
    private final LocalDate today = LocalDate.of(2026, 5, 29);

    @BeforeEach
    void setUp() {
        stats = new MemberStats(Streak.of(3), Streak.of(3), today.minusDays(1));
    }

    @Test
    void 어제_하루_목표를_달성했으면_스트릭이_증가한다() {
        stats.incrementStreak(today);

        assertThat(stats.getCurrentStreak()).isEqualTo(Streak.of(4));
        assertThat(stats.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void 마지막_달성일이_이틀_전이면_스트릭이_1로_초기화된다() {
        MemberStats oldStats = new MemberStats(Streak.of(3), Streak.of(3), today.minusDays(2));

        oldStats.incrementStreak(today);

        assertThat(oldStats.getCurrentStreak()).isEqualTo(Streak.of(1));
        assertThat(oldStats.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void 현재_스트릭이_최대_스트릭_기록을_초과하면_기록이_갱신된다() {
        stats.incrementStreak(today);

        assertThat(stats.getMaxStreak()).isEqualTo(Streak.of(4));
        assertThat(stats.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void 스트릭을_리셋하면_currentStreak이_0이_되고_갱신일은_유지된다() {
        stats.resetStreak();

        assertThat(stats.getCurrentStreak()).isEqualTo(Streak.of(0));
        assertThat(stats.getLastAchievedDate()).isEqualTo(today.minusDays(1));
    }

    @Test
    void 같은_날_다시_달성해도_스트릭은_변하지_않는다() {
        MemberStats sameDay = new MemberStats(Streak.of(5), Streak.of(5), today);

        sameDay.incrementStreak(today);

        assertThat(sameDay.getCurrentStreak()).isEqualTo(Streak.of(5));
        assertThat(sameDay.getLastAchievedDate()).isEqualTo(today);
    }
}
