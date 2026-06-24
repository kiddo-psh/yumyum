package com.ssafy.manager.routine.application;

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
    @Mock ApplicationEventPublisher eventPublisher;

    RoutineSessionService routineSessionService;

    @BeforeEach
    void setUp() {
        routineSessionService = new RoutineSessionService(
                routineRepository,
                routineSessionRepository,
                sessionSetRepository,
                routineAiAdjustService,
                eventPublisher
        );
    }

    @Test
    void 세션_기록시_RoutineSession과_SessionSet이_저장된다() {
        given(routineRepository.existsById(1L)).willReturn(true);
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
    void 세션이_기록되면_WorkoutLoggedEvent가_발행된다() {
        given(routineRepository.existsById(1L)).willReturn(true);
        List<SessionSetInput> inputs = List.of(
                new SessionSetInput(10L, "벤치프레스", 1, 8, 60.0, true)
        );

        routineSessionService.recordSession(2L, 1L, LocalDate.of(2026, 6, 10), inputs);

        verify(eventPublisher).publishEvent(new WorkoutLoggedEvent(2L, LocalDate.of(2026, 6, 10)));
    }

    @Test
    void 없는_루틴으로_세션_기록시_예외가_발생한다() {
        given(routineRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> routineSessionService.recordSession(
                2L, 1L, LocalDate.now(), List.of()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("루틴을 찾을 수 없습니다");
    }
}
