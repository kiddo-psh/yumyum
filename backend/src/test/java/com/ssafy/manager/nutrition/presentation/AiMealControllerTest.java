package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.nutrition.application.AiMealService;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiMealController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class AiMealControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AiMealService aiMealService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    private static final LastMealRecommendResponse RESULT = new LastMealRecommendResponse(
            List.of(new LastMealRecommendResponse.MealRecommendation(
                    "닭가슴살 샐러드", 380.0, 42.0, 18.0, 12.0, "단백질 보충")),
            "protein",
            "단백질이 부족합니다."
    );

    @Test
    void 끼니_추천_성공시_200과_추천_목록_반환() throws Exception {
        given(aiMealService.lastRecommend(any(), any())).willReturn(RESULT);

        mockMvc.perform(post("/ai/meals/last-recommend")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].name").value("닭가슴살 샐러드"))
                .andExpect(jsonPath("$.priorityNutrient").value("protein"))
                .andExpect(jsonPath("$.aiComment").isNotEmpty());
    }

    @Test
    void FastAPI_장애시_503_반환() throws Exception {
        given(aiMealService.lastRecommend(any(), any()))
                .willThrow(new RestClientException("FastAPI 연결 실패"));

        mockMvc.perform(post("/ai/meals/last-recommend")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void 인증_없이_끼니_추천하면_401_반환() throws Exception {
        mockMvc.perform(post("/ai/meals/last-recommend").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 활성_프로그램_없으면_404_반환() throws Exception {
        given(aiMealService.lastRecommend(any(), any()))
                .willThrow(new NoSuchElementException("활성 프로그램이 없습니다."));

        mockMvc.perform(post("/ai/meals/last-recommend")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 식사_기록_없으면_409_반환() throws Exception {
        given(aiMealService.lastRecommend(any(), any()))
                .willThrow(new IllegalStateException("오늘 식사 기록이 없어 추천을 생성할 수 없습니다."));

        mockMvc.perform(post("/ai/meals/last-recommend")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }
}
