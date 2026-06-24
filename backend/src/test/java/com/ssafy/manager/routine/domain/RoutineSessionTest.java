package com.ssafy.manager.routine.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RoutineSessionTest {

    @Test
    void RoutineSession_create_팩토리로_생성된다() {
        RoutineSession session = RoutineSession.create(1L, 2L, LocalDate.of(2026, 6, 10));

        assertThat(session.getRoutineId()).isEqualTo(1L);
        assertThat(session.getMemberId()).isEqualTo(2L);
        assertThat(session.getSessionDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    void SessionSet_create_팩토리로_생성된다() {
        SessionSet set = SessionSet.create(1L, 10L, "벤치프레스", 1, 8, 60.0, true);

        assertThat(set.getSessionId()).isEqualTo(1L);
        assertThat(set.getExerciseId()).isEqualTo(10L);
        assertThat(set.getExerciseName()).isEqualTo("벤치프레스");
        assertThat(set.getSetNumber()).isEqualTo(1);
        assertThat(set.getActualReps()).isEqualTo(8);
        assertThat(set.getActualWeightKg()).isEqualTo(60.0);
        assertThat(set.isCompleted()).isTrue();
    }
}
