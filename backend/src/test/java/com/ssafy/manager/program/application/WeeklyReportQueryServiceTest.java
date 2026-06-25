package com.ssafy.manager.program.application;

import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeeklyReportQueryServiceTest {

    @Mock ProgramRepository programRepository;
    @Mock WeeklyReportRepository weeklyReportRepository;

    @InjectMocks WeeklyReportQueryService weeklyReportQueryService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PROGRAM_ID = 10L;

    private Program programOf(Long memberId) {
        return Program.create(memberId, ProgramType.DIET, LocalDate.now(), LocalDate.now().plusDays(27),
                1800, 130.0, 180.0, 50.0, null);
    }

    @Test
    void 단건_조회_시_소유주이면_결과를_반환한다() {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(programOf(MEMBER_ID)));
        WeeklyReport report = new WeeklyReport(PROGRAM_ID, 1);
        report.fill("코멘트", "영양", "운동", "목표", 99.0, 5, -1.0);
        given(weeklyReportRepository.findByProgramIdAndWeekNumber(PROGRAM_ID, 1)).willReturn(Optional.of(report));

        WeeklyReportResult result = weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1);

        assertThat(result.weekNumber()).isEqualTo(1);
        assertThat(result.content()).isEqualTo("코멘트");
        assertThat(result.avgCalorieRate()).isEqualTo(99.0);
    }

    @Test
    void 단건_조회_시_다른_회원이면_ForbiddenException() {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(programOf(99L)));

        assertThatThrownBy(() -> weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1))
                .isInstanceOf(ForbiddenException.class);
        verify(weeklyReportRepository, never()).findByProgramIdAndWeekNumber(PROGRAM_ID, 1);
    }

    @Test
    void 단건_조회_시_프로그램이_없으면_NoSuchElementException() {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 단건_조회_시_리포트가_없으면_NoSuchElementException() {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(programOf(MEMBER_ID)));
        given(weeklyReportRepository.findByProgramIdAndWeekNumber(PROGRAM_ID, 1)).willReturn(Optional.empty());

        assertThatThrownBy(() -> weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 목록_조회_시_소유주이면_주차_순서대로_반환한다() {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(programOf(MEMBER_ID)));
        WeeklyReport week1 = new WeeklyReport(PROGRAM_ID, 1);
        week1.fill("c1", "n1", "e1", "g1", 99.0, 5, -1.0);
        WeeklyReport week2 = new WeeklyReport(PROGRAM_ID, 2);
        week2.fill("c2", "n2", "e2", "g2", 88.0, 4, -0.5);
        given(weeklyReportRepository.findByProgramIdOrderByWeekNumberAsc(PROGRAM_ID))
                .willReturn(List.of(week1, week2));

        List<WeeklyReportResult> results = weeklyReportQueryService.getReports(MEMBER_ID, PROGRAM_ID);

        assertThat(results).extracting(WeeklyReportResult::weekNumber).containsExactly(1, 2);
    }

    @Test
    void 목록_조회_시_다른_회원이면_ForbiddenException() {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(programOf(99L)));

        assertThatThrownBy(() -> weeklyReportQueryService.getReports(MEMBER_ID, PROGRAM_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(weeklyReportRepository, never()).findByProgramIdOrderByWeekNumberAsc(PROGRAM_ID);
    }
}
