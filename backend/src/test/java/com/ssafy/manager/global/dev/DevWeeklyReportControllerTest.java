package com.ssafy.manager.global.dev;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.program.application.WeeklyReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevWeeklyReportController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class DevWeeklyReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WeeklyReportService weeklyReportService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    @Test
    void 날짜를_받아_createStubs를_호출하고_인증없이_200을_반환한다() throws Exception {
        mockMvc.perform(post("/dev/weekly-reports/run").param("date", "2026-06-25"))
                .andExpect(status().isOk());

        verify(weeklyReportService).createStubs(LocalDate.of(2026, 6, 25));
    }
}
