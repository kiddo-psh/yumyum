package com.ssafy.manager.auth.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "localdev-secret-key-must-be-at-least-32-bytes-long",
                1800000L,
                1209600000L
        );
    }

    @Test
    void 생성한_토큰에서_memberId를_파싱하면_원래_값이_반환된다() {
        String token = jwtProvider.createAccessToken(42L);

        Long memberId = jwtProvider.getMemberId(token);

        assertThat(memberId).isEqualTo(42L);
    }

    @Test
    void 위조된_서명의_토큰을_검증하면_예외가_발생한다() {
        JwtProvider otherProvider = new JwtProvider(
                "different-secret-key-must-be-at-least-32-bytes!!",
                1800000L,
                1209600000L
        );
        String tamperedToken = otherProvider.createAccessToken(42L);

        assertThatThrownBy(() -> jwtProvider.validate(tamperedToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void 만료된_토큰을_검증하면_예외가_발생한다() {
        JwtProvider expiredProvider = new JwtProvider(
                "localdev-secret-key-must-be-at-least-32-bytes-long",
                -1L,
                1209600000L
        );
        String expiredToken = expiredProvider.createAccessToken(42L);

        assertThatThrownBy(() -> jwtProvider.validate(expiredToken))
                .isInstanceOf(RuntimeException.class);
    }
}
