package com.ssafy.manager.growth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StreakTest {

    private static final LocalDate YESTERDAY = LocalDate.of(2026, 6, 9);
    private static final LocalDate TODAY = YESTERDAY.plusDays(1);

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 1_000, 1_000_000, 1_000_000_000})
    void Streak은_1씩_증가한다(int count) {
        Streak streak = Streak.of(count, YESTERDAY);
        Streak incremented = Streak.of(count + 1, TODAY);

        Streak updated = streak.increment(TODAY);

        assertThat(updated).isEqualTo(incremented);
    }

    @Test
    void Streak은_최댓값을_넘길_수_없다() {
        final int OVER_MAX_STREAK_VALUE = Streak.MAX_LIMIT + 1;
        assertThrows(IllegalArgumentException.class, () -> Streak.of(OVER_MAX_STREAK_VALUE, TODAY));
    }

    @ParameterizedTest
    @ValueSource(ints = {Streak.MAX_LIMIT, Integer.MAX_VALUE})
    void Streak은_Integer_오버플로우를_방지한다(int count) {
        assertThrows(IllegalArgumentException.class, () -> {
            Streak streak = Streak.of(count, YESTERDAY);
            streak.increment(TODAY);
        });
    }

    @Test
    void Streak은_0이하라면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> Streak.of(-1, TODAY));
    }

    @Test
    void Streak이_리셋되면_0부터_시작하고_갱신일은_유지된다() {
        Streak streak = Streak.of(10, TODAY);
        Streak start = Streak.of(0, TODAY);

        Streak reset = streak.reset();

        assertThat(reset).isEqualTo(start);
    }

    @Test
    void Streak이_연속으로_갱신되면_날짜도_1증가한다() {
        Streak streak = Streak.of(1, YESTERDAY);
        Streak incremented = Streak.of(2, TODAY);

        Streak updated = streak.increment(TODAY);

        assertThat(updated).isEqualTo(incremented);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 7, 30})
    void 마지막_갱신일이_어제보다_이전이면_날짜가_초기화된다(int offset) {
        Streak streak = Streak.of(10, TODAY.minusDays(offset));
        Streak firstStreak = Streak.of(1, TODAY);

        Streak increment = streak.increment(TODAY);

        assertThat(increment).isEqualTo(firstStreak);
    }

    @Test
    void 같은_날_2번_갱신되어도_스트릭_카운트와_갱신일은_변함없다() {
        Streak streak = Streak.of(10, YESTERDAY);
        Streak expected = Streak.of(11, TODAY);

        Streak increment = streak.increment(TODAY).increment(TODAY);

        assertThat(increment).isEqualTo(expected);
    }
}
