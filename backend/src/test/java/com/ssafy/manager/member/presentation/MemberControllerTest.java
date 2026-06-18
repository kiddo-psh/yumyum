package com.ssafy.manager.member.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.member.application.MemberService;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.domain.ActivityLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class MemberControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MemberService memberService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    private static final String BODY = """
            {"sex":"MALE","birthYear":1990,"heightCm":175.0,"weightKg":80.0,
             "activityLevel":"MODERATELY_ACTIVE","healthGoal":"DIET"}
            """;

    @Test
    void 온보딩_성공시_200과_프로필_반환() throws Exception {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        member.completeOnboarding(Sex.MALE, 1990, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE, HealthGoal.DIET);
        given(memberService.completeOnboarding(eq(MEMBER_ID), any())).willReturn(member);

        mockMvc.perform(patch("/members/me")
                        .with(authentication(AUTH))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.healthGoal").value("DIET"));
    }

    @Test
    void 인증_없이_온보딩하면_401_반환() throws Exception {
        mockMvc.perform(patch("/members/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }
}
