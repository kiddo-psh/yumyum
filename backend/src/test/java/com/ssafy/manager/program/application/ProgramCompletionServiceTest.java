package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProgramCompletionServiceTest {

    @Mock ProgramRepository programRepository;

    @InjectMocks ProgramCompletionService programCompletionService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void endDate가_오늘보다_이전인_Program은_COMPLETED로_전환된다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(28), TODAY.minusDays(1), 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatusAndEndDateBefore(ProgramStatus.ACTIVE, TODAY))
                .willReturn(List.of(program));

        programCompletionService.completeExpired(TODAY);

        assertThat(program.isActive()).isFalse();
        verify(programRepository).save(program);
    }

    @Test
    void endDate가_오늘인_Program은_COMPLETED로_전환되지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(28), TODAY, 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatusAndEndDateBefore(ProgramStatus.ACTIVE, TODAY))
                .willReturn(List.of());

        programCompletionService.completeExpired(TODAY);

        assertThat(program.isActive()).isTrue();
        verify(programRepository, never()).save(program);
    }
}
