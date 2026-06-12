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

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 11);

    @Test
    void memberId_목록을_받아_각_streak을_초기화한다() {
        MemberStats stats = new MemberStats(Streak.of(5), Streak.of(10), TODAY.minusDays(1));
        given(memberStatsRepository.findByMemberId(1L)).willReturn(Optional.of(stats));

        streakResetService.resetFor(List.of(1L));

        assertThat(stats.getCurrentStreak()).isEqualTo(Streak.of(0));
        verify(memberStatsRepository).save(stats);
    }

    @Test
    void MemberStats가_없는_memberId는_무시된다() {
        given(memberStatsRepository.findByMemberId(99L)).willReturn(Optional.empty());

        streakResetService.resetFor(List.of(99L));

        verify(memberStatsRepository, never()).save(any());
    }

    @Test
    void 어제_달성하지_않은_회원의_Streak이_리셋된다() {
        MemberStats expired = new MemberStats(Streak.of(3), Streak.of(10), TODAY.minusDays(2));
        given(memberStatsRepository.findAllWithExpiredStreak(TODAY.minusDays(1))).willReturn(List.of(expired));

        streakResetService.resetUnachievedFor(TODAY);

        assertThat(expired.getCurrentStreak()).isEqualTo(Streak.of(0));
        verify(memberStatsRepository).save(expired);
    }

    @Test
    void 어제_달성한_회원의_Streak은_리셋되지_않는다() {
        given(memberStatsRepository.findAllWithExpiredStreak(TODAY.minusDays(1))).willReturn(List.of());

        streakResetService.resetUnachievedFor(TODAY);

        verify(memberStatsRepository, never()).save(any());
    }
}
