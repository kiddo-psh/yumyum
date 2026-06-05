package com.ssafy.manager.auth.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberOAuthService {

    private final MemberRepository memberRepository;

    public Member getOrRegister(OAuthUserInfo userInfo) {
        return memberRepository.findByOauthProviderAndOauthId(userInfo.provider(), userInfo.oauthId())
                .orElseGet(() -> memberRepository.save(
                        new Member(userInfo.provider(), userInfo.oauthId(), userInfo.email())));
    }
}
