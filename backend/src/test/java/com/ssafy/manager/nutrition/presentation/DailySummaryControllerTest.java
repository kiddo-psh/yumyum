package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.nutrition.application.DailySummaryService;
import com.ssafy.manager.nutrition.presentation.dto.DailySummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DailySummaryController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class DailySummaryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DailySummaryService dailySummaryService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void date_파라미터_없이_조회하면_오늘_기준으로_반환한다() throws Exception {
        given(dailySummaryService.getSummary(any(), any()))
                .willReturn(new DailySummaryResponse(2000, 0, false, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/daily-summary")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk());
    }

    @Test
    void 인증_없이_일일_요약_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/daily-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void PRD_명세_구조로_일일_요약을_반환한다() throws Exception {
        DailySummaryResponse response = new DailySummaryResponse(
                2664, 1200.0, false, 5, 10, 120.0, 80.0, 40.0
        );
        given(dailySummaryService.getSummary(MEMBER_ID, LocalDate.of(2026, 6, 1)))
                .willReturn(response);

        mockMvc.perform(get("/daily-summary")
                        .with(authentication(AUTH))
                        .param("date", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCalories").value(2664))
                .andExpect(jsonPath("$.achievedCalories").value(1200.0))
                .andExpect(jsonPath("$.achieved").value(false))
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.maxStreak").value(10))
                .andExpect(jsonPath("$.totalCarbs").value(120.0))
                .andExpect(jsonPath("$.totalProtein").value(80.0))
                .andExpect(jsonPath("$.totalFat").value(40.0));
    }
}
