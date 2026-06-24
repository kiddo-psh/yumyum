package com.ssafy.manager.home.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.home.application.HomeCommentService;
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

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class HomeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean HomeCommentService homeCommentService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 인증된_사용자는_코멘트를_조회할_수_있다() throws Exception {
        given(homeCommentService.getComment(MEMBER_ID))
                .willReturn("근육 증가 목표, 오늘도 잘 하고 있어요!");

        mockMvc.perform(get("/home/comment").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("근육 증가 목표, 오늘도 잘 하고 있어요!"));
    }

    @Test
    void 인증_없이_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/home/comment"))
                .andExpect(status().isUnauthorized());
    }
}
