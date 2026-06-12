package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientResponse;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @Mock RoutineExerciseRepository routineExerciseRepository;
    @Mock AiRoutineClient aiRoutineClient;

    @InjectMocks RoutineSessionService routineSessionService;

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
    void adjustAndSave_FastAPI응답으로_새_주차_운동이_저장된다() {
        RoutineExercise ex = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);
        given(routineExerciseRepository.findMaxWeekNumberByRoutineId(1L)).willReturn(1);
        given(routineExerciseRepository.findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(1L, 1))
                .willReturn(List.of(ex));
        given(routineSessionRepository.findTop4ByRoutineIdOrderBySessionDateDesc(1L)).willReturn(List.of());
        given(aiRoutineClient.adjust(any())).willReturn(new AiRoutineAdjustClientResponse(
                List.of(new AiRoutineAdjustClientResponse.Adjustment(
                        ex.getId(), "UP", 62.5, 4, 8, "성공 → 증량")),
                "잘 하고 있어요!", 2
        ));

        routineSessionService.adjustAndSave(1L);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(routineExerciseRepository).saveAll(captor.capture());
        List<RoutineExercise> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getWeekNumber()).isEqualTo(2);
        assertThat(saved.get(0).getTargetWeightKg()).isEqualTo(62.5);
    }
}
