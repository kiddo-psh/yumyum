package com.ssafy.manager.program.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.program.application.WeeklyReportQueryService;
import com.ssafy.manager.program.application.WeeklyReportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeeklyReportController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class WeeklyReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WeeklyReportQueryService weeklyReportQueryService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final Long PROGRAM_ID = 10L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 주간_리포트를_정상_조회한다() throws Exception {
        WeeklyReportResult result = new WeeklyReportResult(
                1L, 1, "코멘트", "영양요약", "운동요약", "목표요약", 87.5, 5, -0.2);
        given(weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1)).willReturn(result);

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", PROGRAM_ID)
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
        given(weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1))
                .willThrow(new ForbiddenException("접근 권한이 없습니다."));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void WeeklyReport가_없으면_404를_반환한다() throws Exception {
        given(weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1))
                .willThrow(new NoSuchElementException("WeeklyReport를 찾을 수 없습니다."));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/1", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 프로그램이_존재하지_않으면_404를_반환한다() throws Exception {
        given(weeklyReportQueryService.getReport(MEMBER_ID, PROGRAM_ID, 1))
                .willThrow(new NoSuchElementException("Program을 찾을 수 없습니다."));

        mockMvc.perform(get("/programs/{programId}/weekly-reports/{weekNumber}", PROGRAM_ID, 1)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 리포트_목록을_주차_오름차순으로_조회한다() throws Exception {
        WeeklyReportResult week1 = new WeeklyReportResult(
                1L, 1, "코멘트1", "영양1", "운동1", "목표1", 99.0, 5, -1.0);
        WeeklyReportResult week2 = new WeeklyReportResult(
                2L, 2, "코멘트2", "영양2", "운동2", "목표2", 88.0, 4, -0.5);
        given(weeklyReportQueryService.getReports(MEMBER_ID, PROGRAM_ID))
                .willReturn(List.of(week1, week2));

        mockMvc.perform(get("/programs/{programId}/weekly-reports", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weekNumber").value(1))
                .andExpect(jsonPath("$[0].avgCalorieRate").value(99.0))
                .andExpect(jsonPath("$[1].weekNumber").value(2));
    }

    @Test
    void 목록_조회_시_다른_회원_program_접근은_403() throws Exception {
        given(weeklyReportQueryService.getReports(MEMBER_ID, PROGRAM_ID))
                .willThrow(new ForbiddenException("접근 권한이 없습니다."));

        mockMvc.perform(get("/programs/{programId}/weekly-reports", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 목록_조회_시_프로그램_미존재는_404() throws Exception {
        given(weeklyReportQueryService.getReports(MEMBER_ID, PROGRAM_ID))
                .willThrow(new NoSuchElementException("Program을 찾을 수 없습니다."));

        mockMvc.perform(get("/programs/{programId}/weekly-reports", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 인증_없이_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/programs/10/weekly-reports/1"))
                .andExpect(status().isUnauthorized());
    }
}
