package com.ssafy.manager.growth.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.growth.application.DailyGoalSummaryService;
import com.ssafy.manager.growth.domain.DailyProgress;
import com.ssafy.manager.growth.domain.WeeklyAchievementSummary;
import com.ssafy.manager.member.application.MemberService;
import com.ssafy.manager.member.application.dto.OnboardingResult;
import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.program.domain.DailyGoal;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrowthController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class GrowthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DailyGoalSummaryService dailyGoalSummaryService;
    @MockitoBean MemberService memberService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 인증_없이_주간_캘린더를_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/growth/weekly-calendar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 주간_캘린더를_조회하면_7일_항목을_상태200으로_반환한다() throws Exception {
        LocalDate monday = LocalDate.of(2026, 6, 8);
        List<DailyGoal> goals = List.of(
                DailyGoal.of(MEMBER_ID, monday, 2000),
                DailyGoal.of(MEMBER_ID, monday.plusDays(1), 2000)
        );
        given(dailyGoalSummaryService.weeklyCalendar(any(), any(), anyInt()))
                .willReturn(WeeklyAchievementSummary.of(monday, goals));

        mockMvc.perform(get("/growth/weekly-calendar")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-06-08"))
                .andExpect(jsonPath("$.weekEnd").value("2026-06-14"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days.length()").value(7));
    }

    @Test
    void weekOffset_파라미터를_서비스로_전달한다() throws Exception {
        LocalDate monday = LocalDate.of(2026, 6, 1);
        given(dailyGoalSummaryService.weeklyCalendar(any(), any(), anyInt()))
                .willReturn(WeeklyAchievementSummary.of(monday, List.of()));

        mockMvc.perform(get("/growth/weekly-calendar")
                        .param("weekOffset", "-1")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk());

        verify(dailyGoalSummaryService)
                .weeklyCalendar(eq(MEMBER_ID), any(LocalDate.class), eq(-1));
    }

    @Test
    void weekOffset가_없으면_기본값_0으로_서비스를_호출한다() throws Exception {
        LocalDate monday = LocalDate.of(2026, 6, 8);
        given(dailyGoalSummaryService.weeklyCalendar(any(), any(), anyInt()))
                .willReturn(WeeklyAchievementSummary.of(monday, List.of()));

        mockMvc.perform(get("/growth/weekly-calendar")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk());

        verify(dailyGoalSummaryService)
                .weeklyCalendar(eq(MEMBER_ID), any(LocalDate.class), eq(0));
    }

    @Test
    void 인증_없이_진행바를_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/growth/progress"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 진행바를_조회하면_200과_달성_현황을_반환한다() throws Exception {
        given(dailyGoalSummaryService.todayProgress(any(), any()))
                .willReturn(new DailyProgress(2000.0, 1350.0, 0.675, false));

        mockMvc.perform(get("/growth/progress")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetKcal").value(2000.0))
                .andExpect(jsonPath("$.achievedKcal").value(1350.0))
                .andExpect(jsonPath("$.achievementRate").value(0.675))
                .andExpect(jsonPath("$.achieved").value(false));
    }

    @Test
    void Nyam_상태를_조회하면_200과_상태_반환() throws Exception {
        given(dailyGoalSummaryService.todayProgress(any(), any()))
                .willReturn(new DailyProgress(2000.0, 1700.0, 0.85, true));
        OnboardingResult memberResult = new OnboardingResult(
                MEMBER_ID, true, Sex.MALE, 1990, 175.0, 80.0,
                ActivityLevel.MODERATELY_ACTIVE, HealthGoal.DIET
        );
        given(memberService.getMember(MEMBER_ID)).willReturn(memberResult);

        mockMvc.perform(get("/nyam/status")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mood").value("HAPPY"))
                .andExpect(jsonPath("$.achievementRate").value(0.85))
                .andExpect(jsonPath("$.healthGoal").value("DIET"));
    }

    @Test
    void 인증_없이_Nyam_상태를_조회하면_401_반환() throws Exception {
        mockMvc.perform(get("/nyam/status"))
                .andExpect(status().isUnauthorized());
    }
}
