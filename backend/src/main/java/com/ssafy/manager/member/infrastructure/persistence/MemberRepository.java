package com.ssafy.manager.member.infrastructure.persistence;

import com.ssafy.manager.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    java.util.Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
