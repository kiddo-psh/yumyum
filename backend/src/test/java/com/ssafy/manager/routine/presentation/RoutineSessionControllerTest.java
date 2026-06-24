package com.ssafy.manager.routine.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.growth.application.EarnedBadgeCollector;
import com.ssafy.manager.growth.application.StreakChangeHolder;
import com.ssafy.manager.routine.application.RoutineSessionResult;
import com.ssafy.manager.routine.application.RoutineSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutineSessionController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class RoutineSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RoutineSessionService routineSessionService;
    @MockitoBean EarnedBadgeCollector earnedBadgeCollector;
    @MockitoBean StreakChangeHolder streakChangeHolder;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    private static final RoutineSessionResult RESULT = new RoutineSessionResult(
            1L, 1L, 2L,
            LocalDate.of(2026, 6, 10),
            LocalDateTime.of(2026, 6, 10, 20, 0),
            0,
            List.of(new RoutineSessionResult.SetResult(1L, 10L, "벤치프레스", 1, 8, 60.0, true))
    );

    @Test
    void 세션_기록_성공시_201_반환() throws Exception {
        given(routineSessionService.recordSession(any(), any(), any(), anyInt(), any())).willReturn(RESULT);

        String body = """
                {
                  "routineId": 1,
                  "sessionDate": "2026-06-10",
                  "sets": [
                    {"exerciseId": 10, "exerciseName": "벤치프레스",
                     "setNumber": 1, "actualReps": 8, "actualWeightKg": 60.0, "completed": true}
                  ]
                }
                """;

        mockMvc.perform(post("/routines/sessions")
                        .with(authentication(AUTH))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.sets").isArray())
                .andExpect(jsonPath("$.sets[0].exerciseName").value("벤치프레스"));
    }

    @Test
    void 인증_없이_세션_기록하면_401_반환() throws Exception {
        String body = """
                {"routineId": 1, "sessionDate": "2026-06-10", "sets": []}
                """;

        mockMvc.perform(post("/routines/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 없는_루틴으로_세션_기록시_404_반환() throws Exception {
        given(routineSessionService.recordSession(any(), any(), any(), anyInt(), any()))
                .willThrow(new NoSuchElementException("루틴을 찾을 수 없습니다."));

        String body = """
                {"routineId": 99, "sessionDate": "2026-06-10", "sets": []}
                """;

        mockMvc.perform(post("/routines/sessions")
                        .with(authentication(AUTH))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
