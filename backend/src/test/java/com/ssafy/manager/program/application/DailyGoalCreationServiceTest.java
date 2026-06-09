package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyGoalCreationServiceTest {

    @Mock ProgramRepository programRepository;
    @Mock DailyGoalRepository dailyGoalRepository;

    @InjectMocks DailyGoalCreationService dailyGoalCreationService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void 활성_Program이_있으면_오늘_DailyGoal이_생성된다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(1), TODAY.plusDays(27), 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(dailyGoalRepository.existsByMemberIdAndDate(1L, TODAY)).willReturn(false);

        dailyGoalCreationService.createForActivePrograms(TODAY);

        ArgumentCaptor<DailyGoal> captor = ArgumentCaptor.forClass(DailyGoal.class);
        verify(dailyGoalRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetValue()).isEqualTo(2400.0);
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
    }

    @Test
    void 오늘_DailyGoal이_이미_있으면_중복_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(1), TODAY.plusDays(27), 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(dailyGoalRepository.existsByMemberIdAndDate(1L, TODAY)).willReturn(true);

        dailyGoalCreationService.createForActivePrograms(TODAY);

        verify(dailyGoalRepository, never()).save(any(DailyGoal.class));
    }
}
