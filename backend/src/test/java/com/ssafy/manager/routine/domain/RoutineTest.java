package com.ssafy.manager.routine.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoutineTest {

    @Test
    void Routine_create_팩토리로_생성된다() {
        Routine routine = Routine.create(1L, "내 루틴", 4, false);

        assertThat(routine.getMemberId()).isEqualTo(1L);
        assertThat(routine.getName()).isEqualTo("내 루틴");
        assertThat(routine.getDaysPerWeek()).isEqualTo(4);
        assertThat(routine.isAiGenerated()).isFalse();
        assertThat(routine.getCreatedAt()).isNotNull();
    }

    @Test
    void RoutineExercise_create_팩토리로_생성된다() {
        RoutineExercise ex = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);

        assertThat(ex.getRoutineId()).isEqualTo(1L);
        assertThat(ex.getDayLabel()).isEqualTo("상체");
        assertThat(ex.getExerciseName()).isEqualTo("벤치프레스");
        assertThat(ex.getTargetSets()).isEqualTo(4);
        assertThat(ex.getTargetReps()).isEqualTo(8);
        assertThat(ex.getTargetWeightKg()).isEqualTo(60.0);
        assertThat(ex.getOrderIndex()).isEqualTo(0);
    }

    @Test
    void RoutineExercise_update로_필드가_변경된다() {
        RoutineExercise ex = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);

        ex.update("인클라인 벤치프레스", 3, 10, 55.0);

        assertThat(ex.getExerciseName()).isEqualTo("인클라인 벤치프레스");
        assertThat(ex.getTargetSets()).isEqualTo(3);
        assertThat(ex.getTargetReps()).isEqualTo(10);
        assertThat(ex.getTargetWeightKg()).isEqualTo(55.0);
    }
}
