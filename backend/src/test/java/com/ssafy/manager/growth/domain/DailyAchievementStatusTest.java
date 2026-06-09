package com.ssafy.manager.growth.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DailyAchievementStatusTest {

    @Test
    void 달성률이_1_이상이면_ACHIEVED() {
        assertThat(DailyAchievementStatus.of(1.0)).isEqualTo(DailyAchievementStatus.ACHIEVED);
        assertThat(DailyAchievementStatus.of(1.2)).isEqualTo(DailyAchievementStatus.ACHIEVED);
    }

    @Test
    void 달성률이_0_초과_1_미만이면_PARTIAL() {
        assertThat(DailyAchievementStatus.of(0.5)).isEqualTo(DailyAchievementStatus.PARTIAL);
        assertThat(DailyAchievementStatus.of(0.99)).isEqualTo(DailyAchievementStatus.PARTIAL);
    }

    @Test
    void 달성률이_0이면_NONE() {
        assertThat(DailyAchievementStatus.of(0.0)).isEqualTo(DailyAchievementStatus.NONE);
    }
}
