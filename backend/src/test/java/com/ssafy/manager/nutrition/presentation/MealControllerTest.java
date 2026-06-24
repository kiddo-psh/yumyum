package com.ssafy.manager.nutrition.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.growth.application.EarnedBadgeCollector;
import com.ssafy.manager.growth.application.StreakChangeHolder;
import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.domain.MealType;
import com.ssafy.manager.nutrition.presentation.dto.MealRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class MealControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MealService mealService;
    @MockitoBean EarnedBadgeCollector earnedBadgeCollector;
    @MockitoBean StreakChangeHolder streakChangeHolder;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 식사를_기록하면_201과_Location_헤더와_응답_바디를_반환한다() throws Exception {
        Meal meal = mock(Meal.class);
        given(meal.getId()).willReturn(1L);
        given(meal.getType()).willReturn(MealType.LUNCH);
        given(meal.getDate()).willReturn(LocalDate.of(2026, 6, 1));
        given(meal.getItems()).willReturn(List.of());
        given(mealService.record(any(), any())).willReturn(meal);

        MealRequest request = new MealRequest(
                MealType.LUNCH,
                LocalDate.of(2026, 6, 1),
                List.of(new MealRequest.Item("D000001", 100.0))
        );

        mockMvc.perform(post("/meals")
                        .with(authentication(AUTH))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/meals/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("LUNCH"));
    }

    @Test
    void 인증_없이_식사_기록하면_401을_반환한다() throws Exception {
        MealRequest request = new MealRequest(
                MealType.LUNCH,
                LocalDate.of(2026, 6, 1),
                List.of(new MealRequest.Item("D000001", 100.0))
        );

        mockMvc.perform(post("/meals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 날짜로_식사_목록을_조회한다() throws Exception {
        Meal meal = new Meal(MEMBER_ID, MealType.LUNCH, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
        given(mealService.listByDate(MEMBER_ID, LocalDate.of(2026, 6, 1)))
                .willReturn(List.of(meal));

        mockMvc.perform(get("/meals")
                        .with(authentication(AUTH))
                        .param("date", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("LUNCH"));
    }

    @Test
    void 인증_없이_식사_목록_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/meals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void date_파라미터_없이_조회하면_오늘_기준으로_반환한다() throws Exception {
        given(mealService.listByDate(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/meals")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk());
    }
}
