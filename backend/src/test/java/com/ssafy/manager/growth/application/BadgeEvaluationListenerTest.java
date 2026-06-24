package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.growth.infrastructure.persistence.MemberBadgeRepository;
import com.ssafy.manager.routine.application.WorkoutLoggedEvent;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BadgeEvaluationListenerTest {

    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock MemberBadgeRepository memberBadgeRepository;
    @Mock EarnedBadgeCollector earnedBadgeCollector;

    private static final long MEMBER_ID = 1L;
    private final WorkoutLoggedEvent EVENT = new WorkoutLoggedEvent(MEMBER_ID, LocalDate.of(2026, 6, 20));

    private BadgeEvaluationListener listener() {
        BadgeService badgeService = new BadgeService(memberBadgeRepository, earnedBadgeCollector);
        return new BadgeEvaluationListener(badgeService, routineSessionRepository);
    }

    @Test
    void 운동_세션_100회_도달시_올라잇_발행() {
        given(routineSessionRepository.countByMemberId(MEMBER_ID)).willReturn(100L);
        given(routineSessionRepository.countWeekendByMemberId(MEMBER_ID)).willReturn(0L);
        given(memberBadgeRepository.existsByMemberIdAndBadge(MEMBER_ID, Badge.ALL_RIGHT)).willReturn(false);

        listener().on(EVENT);

        verify(memberBadgeRepository).save(org.mockito.ArgumentMatchers.argThat(
                mb -> mb.getBadge() == Badge.ALL_RIGHT && mb.getMemberId().equals(MEMBER_ID)));
        verify(earnedBadgeCollector).add(Badge.ALL_RIGHT);
    }

    @Test
    void 주말_세션_10회_도달시_주말전사_발행() {
        given(routineSessionRepository.countByMemberId(MEMBER_ID)).willReturn(12L);
        given(routineSessionRepository.countWeekendByMemberId(MEMBER_ID)).willReturn(10L);
        given(memberBadgeRepository.existsByMemberIdAndBadge(MEMBER_ID, Badge.WEEKEND_WARRIOR)).willReturn(false);

        listener().on(EVENT);

        verify(earnedBadgeCollector).add(Badge.WEEKEND_WARRIOR);
    }

    @Test
    void 임계_미달시_아무것도_발행하지_않는다() {
        given(routineSessionRepository.countByMemberId(MEMBER_ID)).willReturn(5L);
        given(routineSessionRepository.countWeekendByMemberId(MEMBER_ID)).willReturn(3L);

        listener().on(EVENT);

        verify(memberBadgeRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(earnedBadgeCollector, never()).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 이미_보유한_뱃지는_재발행하지_않는다() {
        given(routineSessionRepository.countByMemberId(MEMBER_ID)).willReturn(150L);
        given(routineSessionRepository.countWeekendByMemberId(MEMBER_ID)).willReturn(0L);
        given(memberBadgeRepository.existsByMemberIdAndBadge(MEMBER_ID, Badge.ALL_RIGHT)).willReturn(true);

        listener().on(EVENT);

        verify(memberBadgeRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(earnedBadgeCollector, never()).add(eq(Badge.ALL_RIGHT));
    }
}
