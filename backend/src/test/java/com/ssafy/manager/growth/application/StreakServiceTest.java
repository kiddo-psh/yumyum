package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.domain.Streak;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    MemberStatsRepository memberStatsRepository;

    @InjectMocks
    StreakService streakService;

    private final LocalDate today = LocalDate.of(2026, 5, 29);

    @Test
    void increment_호출_시_streak이_증가한_상태로_저장된다() {
        MemberStats stats = new MemberStats(Streak.of(3), Streak.of(3), today.minusDays(1));
        given(memberStatsRepository.findByMemberId(1L))
                .willReturn(Optional.of(stats));

        streakService.increment(1L, today);

        assertThat(stats.getCurrentStreak()).isEqualTo(Streak.of(4));
        verify(memberStatsRepository).save(stats);
    }
}
