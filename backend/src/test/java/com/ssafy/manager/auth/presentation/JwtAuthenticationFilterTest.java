package com.ssafy.manager.auth.presentation;

import com.ssafy.manager.auth.infrastructure.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtProvider jwtProvider;
    @Mock FilterChain filterChain;

    JwtAuthenticationFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Bearer_토큰이면_SecurityContext에_memberId가_설정된다() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        given(jwtProvider.getMemberId("valid-token")).willReturn(42L);

        filter.doFilterInternal(request, response, filterChain);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal).isEqualTo(42L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰이면_401을_반환하고_필터_체인을_중단한다() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-token");
        willThrow(new JwtException("invalid")).given(jwtProvider).validate("invalid-token");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }
}
