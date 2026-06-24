package com.ssafy.manager.weight.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.weight.application.WeightService;
import com.ssafy.manager.weight.domain.Weight;
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
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeightController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class WeightControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean WeightService weightService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 체중_등록_성공시_201과_Location_헤더_반환() throws Exception {
        Weight saved = Weight.create(MEMBER_ID, 72.5, LocalDate.of(2026, 6, 10));
        given(weightService.record(eq(MEMBER_ID), anyDouble(), any())).willReturn(saved);

        String body = """
                {"weightKg": 72.5, "recordedDate": "2026-06-10"}
                """;
        mockMvc.perform(post("/weights")
                        .with(authentication(AUTH))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.weightKg").value(72.5));
    }

    @Test
    void 인증_없이_체중_등록하면_401_반환() throws Exception {
        String body = """
                {"weightKg": 72.5, "recordedDate": "2026-06-10"}
                """;
        mockMvc.perform(post("/weights")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 체중_목록_조회_성공시_200_반환() throws Exception {
        Weight w = Weight.create(MEMBER_ID, 72.5, LocalDate.of(2026, 6, 10));
        given(weightService.findByMember(MEMBER_ID)).willReturn(List.of(w));

        mockMvc.perform(get("/weights").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].weightKg").value(72.5));
    }

    @Test
    void 체중_삭제_성공시_204_반환() throws Exception {
        mockMvc.perform(delete("/weights/1")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void 없는_체중_삭제시_404_반환() throws Exception {
        willThrow(new NoSuchElementException("체중 기록을 찾을 수 없습니다."))
                .given(weightService).delete(eq(MEMBER_ID), eq(99L));

        mockMvc.perform(delete("/weights/99")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 타인_체중_삭제시_403_반환() throws Exception {
        willThrow(new ForbiddenException("본인의 체중 기록만 삭제할 수 있습니다."))
                .given(weightService).delete(eq(MEMBER_ID), eq(42L));

        mockMvc.perform(delete("/weights/42")
                        .with(authentication(AUTH))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
