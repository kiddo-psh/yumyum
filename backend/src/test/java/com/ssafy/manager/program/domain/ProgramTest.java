package com.ssafy.manager.program.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramTest {

    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final LocalDate END = LocalDate.of(2026, 6, 28);

    @Test
    void 생성된_Program은_ACTIVE_상태이며_targetCalories를_보관한다() {
        Program program = Program.create(1L, ProgramType.DIET, START, END, 2164);

        assertThat(program.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(program.getTargetCalories()).isEqualTo(2164);
    }

    @Test
    void complete_호출_시_COMPLETED_상태로_전환된다() {
        Program program = Program.create(1L, ProgramType.DIET, START, END, 2164);

        program.complete();

        assertThat(program.getStatus()).isEqualTo(ProgramStatus.COMPLETED);
    }

    @Test
    void ACTIVE_Program은_isActive가_true이다() {
        Program program = Program.create(1L, ProgramType.MUSCLE, START, END, 2964);

        assertThat(program.isActive()).isTrue();
    }

    @Test
    void COMPLETED_Program은_isActive가_false이다() {
        Program program = Program.create(1L, ProgramType.MUSCLE, START, END, 2964);
        program.complete();

        assertThat(program.isActive()).isFalse();
    }
}
