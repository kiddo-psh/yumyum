package com.ssafy.manager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MemberStatsTest {

    private MemberStats stats;
    private final LocalDate today = LocalDate.of(2026, 5, 29);

    @BeforeEach
    void setUp() {
        stats = new MemberStats(3, 3, today.minusDays(1));
    }

    @Test
    void 어제_하루_목표를_달성했으면_스트릭이_증가한다() {
        stats.incrementStreak(today);

        assertThat(stats.getCurrentStreak()).isEqualTo(4);
        assertThat(stats.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void 마지막_달성일이_이틀_전이면_스트릭이_1로_초기화된다() {
        MemberStats oldStats = new MemberStats(3, 3, today.minusDays(2));

        oldStats.incrementStreak(today);

        assertThat(oldStats.getCurrentStreak()).isEqualTo(1);
        assertThat(oldStats.getLastAchievedDate()).isEqualTo(today);
    }

    @Test
    void 현재_스트릭이_최대_스트릭_기록을_초과하면_기록이_갱신된다() {
        stats.incrementStreak(today);

        assertThat(stats.getMaxStreak()).isEqualTo(4);
        assertThat(stats.getLastAchievedDate()).isEqualTo(today);
    }
}