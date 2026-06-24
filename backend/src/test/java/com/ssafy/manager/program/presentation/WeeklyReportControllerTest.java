package com.ssafy.manager.program.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeeklyReportController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class WeeklyReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProgramRepository programRepository;
    @MockitoBean WeeklyReportRepository weeklyReportRepository;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final Long PROGRAM_ID = 10L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 주간_리포트를_정상_조회한다() throws Exception {
        Long programId = 10L;
        Program program = Program.create(MEMBER_ID, ProgramType.MUSCLE, LocalDate.now(), LocalDate.now().plusDays(27),
                2400, 120.0, 250.0, 60.0, null);
        given(programRepository.findById(programId)).willReturn(Optional.of(program));

        WeeklyReport report = new WeeklyReport(programId, 1);
        report.fill("코멘트", "영양요약", "운동요약", "목표요약", 87.5, 5, -0.2);
        given(weeklyReportRepository.findByProgramIdAndWeekNumber(programId, 1))
                .willReturn(Optional.of(report));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", programId)
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekNumber").value(1))
                .andExpect(jsonPath("$.content").value("코멘트"))
                .andExpect(jsonPath("$.nutritionSummary").value("영양요약"))
                .andExpect(jsonPath("$.avgCalorieRate").value(87.5))
                .andExpect(jsonPath("$.achievementDays").value(5))
                .andExpect(jsonPath("$.weightTrend").value(-0.2));
    }

    @Test
    void 다른_회원_program_접근_시_403을_반환한다() throws Exception {
        Long programId = 10L;
        Program program = Program.create(99L, ProgramType.DIET, LocalDate.now(), LocalDate.now().plusDays(27),
                1800, 90.0, 200.0, 50.0, null);
        given(programRepository.findById(programId)).willReturn(Optional.of(program));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", programId)
                        .with(authentication(AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void WeeklyReport가_없으면_404를_반환한다() throws Exception {
        Long programId = 10L;
        Program program = Program.create(MEMBER_ID, ProgramType.HEALTH, LocalDate.now(), LocalDate.now().plusDays(27),
                2200, 100.0, 230.0, 55.0, null);
        given(programRepository.findById(programId)).willReturn(Optional.of(program));
        given(weeklyReportRepository.findByProgramIdAndWeekNumber(programId, 1))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", programId)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 프로그램이_존재하지_않으면_404를_반환한다() throws Exception {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/programs/{programId}/weekly-reports/{weekNumber}", PROGRAM_ID, 1)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 인증_없이_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/programs/10/weekly-reports/1"))
                .andExpect(status().isUnauthorized());
    }
}
