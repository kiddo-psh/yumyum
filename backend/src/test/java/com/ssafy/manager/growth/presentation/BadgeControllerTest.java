package com.ssafy.manager.growth.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.growth.application.BadgeCollectionResult;
import com.ssafy.manager.growth.application.BadgeCollectionService;
import com.ssafy.manager.growth.domain.Badge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BadgeController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class BadgeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean BadgeCollectionService badgeCollectionService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 컬렉션_조회시_획득과_잠긴_뱃지를_구분해_반환한다() throws Exception {
        BadgeCollectionResult result = new BadgeCollectionResult(List.of(
                new BadgeCollectionResult.Item(Badge.ALL_RIGHT, true, LocalDateTime.of(2026, 6, 20, 12, 0)),
                new BadgeCollectionResult.Item(Badge.WEEKEND_WARRIOR, false, null)
        ));
        given(badgeCollectionService.collectionOf(any())).willReturn(result);

        mockMvc.perform(get("/badges").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.badges[0].code").value("ALL_RIGHT"))
                .andExpect(jsonPath("$.badges[0].earned").value(true))
                .andExpect(jsonPath("$.badges[0].earnedAt").exists())
                // 잠긴 뱃지도 이름·설명이 공개된다 (조건 공개)
                .andExpect(jsonPath("$.badges[1].code").value("WEEKEND_WARRIOR"))
                .andExpect(jsonPath("$.badges[1].earned").value(false))
                .andExpect(jsonPath("$.badges[1].earnedAt").doesNotExist())
                .andExpect(jsonPath("$.badges[1].name").value("주말 전사"))
                .andExpect(jsonPath("$.badges[1].description").isNotEmpty());
    }

    @Test
    void 인증_없이_조회하면_401_반환() throws Exception {
        mockMvc.perform(get("/badges"))
                .andExpect(status().isUnauthorized());
    }
}
