package com.ssafy.manager.routine.application;

import com.ssafy.manager.growth.application.StreakService;
import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.domain.Streak;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoutineSessionServiceTest {

    @Mock RoutineRepository routineRepository;
    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock SessionSetRepository sessionSetRepository;
    @Mock RoutineAiAdjustService routineAiAdjustService;
    @Mock MemberStatsRepository memberStatsRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    RoutineSessionService routineSessionService;

    private final LocalDate YESTERDAY = LocalDate.of(2026, 6, 21);
    private final LocalDate TODAY = YESTERDAY.plusDays(1);
    private final long MEMBER_ID = 1L;
    private final long ROUTINE_ID = 1L;

    @BeforeEach
    void setUp() {
        routineSessionService = new RoutineSessionService(
                routineRepository,
                routineSessionRepository,
                sessionSetRepository,
                routineAiAdjustService,
                streakService(),
                eventPublisher
        );
    }

    @Test
    void 세션_기록시_RoutineSession과_SessionSet이_저장된다() {
        given(routineRepository.existsById(1L)).willReturn(true);
        given(memberStatsRepository.findByMemberId(2L)).willReturn(Optional.of(MemberStats.newFor(2L)));
        List<SessionSetInput> inputs = List.of(
                new SessionSetInput(10L, "벤치프레스", 1, 8, 60.0, true)
        );

        routineSessionService.recordSession(2L, 1L, LocalDate.of(2026, 6, 10), inputs);

        ArgumentCaptor<RoutineSession> captor = ArgumentCaptor.forClass(RoutineSession.class);
        verify(routineSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutineId()).isEqualTo(1L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(2L);
        verify(sessionSetRepository).saveAll(any());
        verify(routineAiAdjustService).adjustAndSave(1L);
    }

    @Test
    void 없는_루틴으로_세션_기록시_예외가_발생한다() {
        given(routineRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> routineSessionService.recordSession(
                2L, 1L, LocalDate.now(), List.of()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("루틴을 찾을 수 없습니다");
    }

    @Test
    void 세션이_기록되면_스트릭이_갱신된다() {
        MemberStats memberStats = new MemberStats(Streak.of(10), Streak.of(10), YESTERDAY);
        List<SessionSetInput> sessionRecords = List.of(
                new SessionSetInput(10L, "벤치프레스", 1, 8, 60.0, true)
        );

        given(routineRepository.existsById(1L)).willReturn(true);
        given(memberStatsRepository.findByMemberId(1L)).willReturn(Optional.of(memberStats));

        routineSessionService.recordSession(MEMBER_ID, ROUTINE_ID, TODAY, 0, sessionRecords);

        assertThat(memberStats.getCurrentStreak().count()).isEqualTo(11);
        assertThat(memberStats.getLastAchievedDate()).isEqualTo(TODAY);
    }

    private StreakService streakService() {
        return new StreakService(memberStatsRepository);
    }
}
