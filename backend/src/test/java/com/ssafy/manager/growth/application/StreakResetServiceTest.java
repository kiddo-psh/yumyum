package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StreakResetServiceTest {

    @Mock MemberStatsRepository memberStatsRepository;

    @InjectMocks StreakResetService streakResetService;

    private static final LocalDate YESTERDAY = LocalDate.of(2026, 6, 1);

    @Test
    void memberId_목록을_받아_각_streak을_초기화한다() {
        MemberStats stats = new MemberStats(5, 10, YESTERDAY);
        given(memberStatsRepository.findByMemberId(1L)).willReturn(Optional.of(stats));

        streakResetService.resetFor(List.of(1L));

        assertThat(stats.getCurrentStreak()).isZero();
        verify(memberStatsRepository).save(stats);
    }

    @Test
    void MemberStats가_없는_memberId는_무시된다() {
        given(memberStatsRepository.findByMemberId(99L)).willReturn(Optional.empty());

        streakResetService.resetFor(List.of(99L));

        verify(memberStatsRepository, never()).save(any());
    }
}
