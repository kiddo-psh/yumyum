package com.ssafy.manager.auth.infrastructure;

import com.ssafy.manager.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteAllByMemberId(Long memberId);
    Optional<RefreshToken> findByToken(String token);
}
