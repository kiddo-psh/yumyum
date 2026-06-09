package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
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
class WeeklyReportServiceTest {

    @Mock ProgramRepository programRepository;
    @Mock WeeklyReportRepository weeklyReportRepository;

    @InjectMocks WeeklyReportService weeklyReportService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void Program_시작_7일_경과_시_WeeklyReport_stub이_생성된다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21), 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(false);

        weeklyReportService.createStubs(TODAY);

        ArgumentCaptor<WeeklyReport> captor = ArgumentCaptor.forClass(WeeklyReport.class);
        verify(weeklyReportRepository).save(captor.capture());
        assertThat(captor.getValue().getWeekNumber()).isEqualTo(1);
        assertThat(captor.getValue().getContent()).isNull();
    }

    @Test
    void WeeklyReport가_이미_있으면_중복_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21), 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(true);

        weeklyReportService.createStubs(TODAY);

        verify(weeklyReportRepository, never()).save(any(WeeklyReport.class));
    }

    @Test
    void 경과일이_7일_미만이면_WeeklyReport를_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(6), TODAY.plusDays(21), 2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));

        weeklyReportService.createStubs(TODAY);

        verify(weeklyReportRepository, never()).save(any(WeeklyReport.class));
    }
}
