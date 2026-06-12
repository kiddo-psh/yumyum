package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.nutrition.application.FoodService;
import com.ssafy.manager.nutrition.domain.Food;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FoodController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class FoodControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FoodService foodService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, List.of());

    @Test
    void query가_없으면_전체_목록을_반환한다() throws Exception {
        given(foodService.search("")).willReturn(List.of(
                new Food("D000001", "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0),
                new Food("D000002", "현미밥", 100.0, 22.0, 2.5, 0.8, 1.5)
        ));

        mockMvc.perform(get("/foods")
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 키워드로_음식_목록을_반환한다() throws Exception {
        given(foodService.search("닭")).willReturn(List.of(
                new Food("D000001", "닭가슴살", 165.0, 0.0, 31.0, 3.6, 0.0)
        ));

        mockMvc.perform(get("/foods")
                        .with(authentication(AUTH))
                        .param("query", "닭"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foodCode").value("D000001"))
                .andExpect(jsonPath("$[0].name").value("닭가슴살"))
                .andExpect(jsonPath("$[0].caloriesPer100g").value(165.0));
    }

    @Test
    void 인증_없이_음식_검색하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/foods"))
                .andExpect(status().isUnauthorized());
    }
}
