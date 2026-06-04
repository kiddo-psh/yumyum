package com.ssafy.manager.auth.application;

import com.ssafy.manager.auth.infrastructure.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }
}
