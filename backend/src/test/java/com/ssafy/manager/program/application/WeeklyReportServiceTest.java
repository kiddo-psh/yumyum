package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClient;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientResponse;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock ProgramRepository programRepository;
    @Mock WeeklyReportRepository weeklyReportRepository;
    @Mock WeeklyCoachingDataService weeklyCoachingDataService;
    @Mock AiCoachingClient aiCoachingClient;

    @InjectMocks WeeklyReportService weeklyReportService;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void Program_시작_7일_경과_시_stub이_생성되고_AI가_호출된다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(false);

        WeeklyReport stub = new WeeklyReport(null, 1);
        given(weeklyReportRepository.save(any(WeeklyReport.class))).willReturn(stub);

        AiCoachingClientRequest mockRequest = mock(AiCoachingClientRequest.class);
        given(weeklyCoachingDataService.buildRequest(any(), eq(1))).willReturn(mockRequest);

        AiCoachingClientResponse mockResponse = new AiCoachingClientResponse(
                "코멘트", "영양요약", "운동요약", "목표요약", 85.0, 5, -0.2);
        given(aiCoachingClient.weekly(mockRequest)).willReturn(mockResponse);

        weeklyReportService.createStubs(TODAY);

        verify(weeklyCoachingDataService).buildRequest(any(), eq(1));
        verify(aiCoachingClient).weekly(mockRequest);
        verify(weeklyReportRepository, times(2)).save(any(WeeklyReport.class));
    }

    @Test
    void AI_호출_실패_시_stub만_저장되고_예외는_전파되지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(false);

        WeeklyReport stub = new WeeklyReport(null, 1);
        given(weeklyReportRepository.save(any(WeeklyReport.class))).willReturn(stub);
        given(weeklyCoachingDataService.buildRequest(any(), anyInt()))
                .willThrow(new RuntimeException("FastAPI 연결 실패"));

        weeklyReportService.createStubs(TODAY);  // 예외 전파 없이 정상 종료

        // stub은 1번 저장, AI 실패 후 두 번째 save 없음
        verify(weeklyReportRepository, times(1)).save(any(WeeklyReport.class));
        assertThat(stub.getContent()).isNull();
    }

    @Test
    void WeeklyReport가_이미_있으면_중복_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(7), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));
        given(weeklyReportRepository.existsByProgramIdAndWeekNumber(null, 1)).willReturn(true);

        weeklyReportService.createStubs(TODAY);

        verify(weeklyReportRepository, never()).save(any(WeeklyReport.class));
        verifyNoInteractions(weeklyCoachingDataService, aiCoachingClient);
    }

    @Test
    void 경과일이_7일_미만이면_WeeklyReport를_생성하지_않는다() {
        Program program = Program.create(1L, ProgramType.HEALTH, TODAY.minusDays(6), TODAY.plusDays(21),
                2400, 0, 0, 0, null);
        given(programRepository.findAllByStatus(ProgramStatus.ACTIVE)).willReturn(List.of(program));

        weeklyReportService.createStubs(TODAY);

        verify(weeklyReportRepository, never()).save(any(WeeklyReport.class));
        verifyNoInteractions(weeklyCoachingDataService, aiCoachingClient);
    }
}
