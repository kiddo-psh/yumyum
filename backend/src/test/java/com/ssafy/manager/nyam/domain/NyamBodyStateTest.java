package com.ssafy.manager.nyam.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NyamBodyStateTest {

    private final LocalDate anchorDate = LocalDate.of(2026, 6, 1);

    @Test
    void 생성_직후_가상_체중은_anchor와_같다() {
        NyamBodyState state = NyamBodyState.newFor(1L, 70.0, anchorDate);

        assertThat(state.virtualWeightKg()).isEqualTo(70.0);
        assertThat(state.getCumulativeBalanceKcal()).isZero();
    }

    @Test
    void 칼로리_잉여_7700kcal_누적시_가상체중이_1kg_증가한다() {
        NyamBodyState state = NyamBodyState.newFor(1L, 70.0, anchorDate);

        state.applyDailyBalance(7700);

        assertThat(state.virtualWeightKg()).isEqualTo(71.0);
    }

    @Test
    void 칼로리_결손은_가상체중을_감소시킨다() {
        NyamBodyState state = NyamBodyState.newFor(1L, 70.0, anchorDate);

        state.applyDailyBalance(-7700);

        assertThat(state.virtualWeightKg()).isEqualTo(69.0);
    }

    @Test
    void 일별_밸런스는_누적된다() {
        NyamBodyState state = NyamBodyState.newFor(1L, 70.0, anchorDate);

        state.applyDailyBalance(3850);
        state.applyDailyBalance(3850);

        assertThat(state.virtualWeightKg()).isEqualTo(71.0);
    }

    @Test
    void 체중_기록시_anchor가_리셋되고_누적치가_비워진다() {
        NyamBodyState state = NyamBodyState.newFor(1L, 70.0, anchorDate);
        state.applyDailyBalance(7700);

        LocalDate newDate = LocalDate.of(2026, 6, 10);
        state.resetAnchor(68.0, newDate);

        assertThat(state.getAnchorWeightKg()).isEqualTo(68.0);
        assertThat(state.getAnchorDate()).isEqualTo(newDate);
        assertThat(state.getCumulativeBalanceKcal()).isZero();
        assertThat(state.virtualWeightKg()).isEqualTo(68.0);
    }
}
