package com.ssafy.manager.auth.presentation;

import com.ssafy.manager.auth.application.AuthService;
import com.ssafy.manager.auth.application.UnauthorizedException;
import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    @Test
    void 인증된_사용자가_로그아웃하면_204를_반환한다() throws Exception {
        Long memberId = 1L;
        var authentication = new UsernamePasswordAuthenticationToken(memberId, null, List.of());

        mockMvc.perform(post("/auth/logout")
                        .with(authentication(authentication))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(authService).logout(memberId);
    }

    @Test
    void 인증_없이_로그아웃하면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_RefreshToken으로_재발급하면_200과_새_토큰_쌍을_반환한다() throws Exception {
        given(authService.reissue("valid-refresh-token"))
                .willReturn(new ReissueResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"valid-refresh-token\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void 유효하지_않은_RefreshToken으로_재발급하면_401을_반환한다() throws Exception {
        given(authService.reissue("invalid-token"))
                .willThrow(new UnauthorizedException("유효하지 않은 Refresh Token입니다."));

        mockMvc.perform(post("/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"invalid-token\"}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
