package com.ssafy.manager.growth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StreakTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 1_000, 1_000_000, 1_000_000_000})
    void Streak은_1씩_증가한다(int count) {
        assertThat(Streak.of(count).increment()).isEqualTo(Streak.of(count + 1));
    }

    @Test
    void Streak은_최댓값을_넘길_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> Streak.of(Streak.MAX_LIMIT + 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {Streak.MAX_LIMIT, Integer.MAX_VALUE})
    void Streak은_Integer_오버플로우를_방지한다(int count) {
        assertThrows(IllegalArgumentException.class, () -> Streak.of(count).increment());
    }

    @Test
    void Streak은_0이하라면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class, () -> Streak.of(-1));
    }
}
