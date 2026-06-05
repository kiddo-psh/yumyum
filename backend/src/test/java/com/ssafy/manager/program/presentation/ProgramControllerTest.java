package com.ssafy.manager.program.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.program.application.ProgramService;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.presentation.dto.ProgramRequest;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class ProgramControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ProgramService programService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 프로그램을_생성하면_201과_Location_헤더를_반환한다() throws Exception {
        given(programService.create(MEMBER_ID, ProgramType.DIET,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))).willReturn(1L);

        ProgramRequest request = new ProgramRequest(
                ProgramType.DIET,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        mockMvc.perform(post("/programs")
                        .with(authentication(AUTH))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/programs/1"));
    }

    @Test
    void 인증_없이_프로그램_생성하면_401을_반환한다() throws Exception {
        ProgramRequest request = new ProgramRequest(
                ProgramType.DIET,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        mockMvc.perform(post("/programs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
