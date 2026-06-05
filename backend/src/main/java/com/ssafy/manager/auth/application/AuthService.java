package com.ssafy.manager.auth.application;

import com.ssafy.manager.auth.domain.RefreshToken;
import com.ssafy.manager.auth.infrastructure.JwtProvider;
import com.ssafy.manager.auth.infrastructure.RefreshTokenRepository;
import com.ssafy.manager.auth.presentation.ReissueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }

    @Transactional
    public ReissueResponse reissue(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 Refresh Token입니다."));

        refreshTokenRepository.delete(stored);

        if (stored.isExpired()) {
            throw new UnauthorizedException("만료된 Refresh Token입니다.");
        }

        Long memberId = stored.getMemberId();
        String newAccessToken = jwtProvider.createAccessToken(memberId);
        String newRefreshToken = jwtProvider.createRefreshToken(memberId);
        Instant expiresAt = jwtProvider.getRefreshTokenExpiry();

        refreshTokenRepository.save(new RefreshToken(memberId, newRefreshToken, expiresAt));

        return new ReissueResponse(newAccessToken, newRefreshToken);
    }
}
