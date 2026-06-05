package com.ssafy.manager.auth.application;

import com.ssafy.manager.auth.domain.RefreshToken;
import com.ssafy.manager.auth.infrastructure.JwtProvider;
import com.ssafy.manager.auth.infrastructure.RefreshTokenRepository;
import com.ssafy.manager.auth.presentation.ReissueResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtProvider jwtProvider;

    @InjectMocks AuthService authService;

    @Test
    void 유효한_RefreshToken으로_재발급하면_새_토큰_쌍을_반환한다() {
        Long memberId = 1L;
        String oldToken = "old-refresh-token";
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        RefreshToken stored = new RefreshToken(memberId, oldToken, futureExpiry);

        given(refreshTokenRepository.findByToken(oldToken)).willReturn(Optional.of(stored));
        given(jwtProvider.createAccessToken(memberId)).willReturn("new-access-token");
        given(jwtProvider.createRefreshToken(memberId)).willReturn("new-refresh-token");
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(futureExpiry);

        ReissueResponse response = authService.reissue(oldToken);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 존재하지_않는_RefreshToken으로_재발급하면_401_예외를_던진다() {
        given(refreshTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue("unknown-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 만료된_RefreshToken으로_재발급하면_401_예외를_던지고_DB에서_삭제한다() {
        Long memberId = 1L;
        String expiredToken = "expired-refresh-token";
        Instant pastExpiry = Instant.now().minusSeconds(1);
        RefreshToken expired = new RefreshToken(memberId, expiredToken, pastExpiry);

        given(refreshTokenRepository.findByToken(expiredToken)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.reissue(expiredToken))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenRepository).delete(expired);
    }
}
