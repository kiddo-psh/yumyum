package com.ssafy.manager.weight.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class WeightTest {

    @Test
    void Weight_create_팩토리로_생성된다() {
        Weight w = Weight.create(1L, 72.5, LocalDate.of(2026, 6, 10));

        assertThat(w.getMemberId()).isEqualTo(1L);
        assertThat(w.getWeightKg()).isEqualTo(72.5);
        assertThat(w.getRecordedDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }
}
