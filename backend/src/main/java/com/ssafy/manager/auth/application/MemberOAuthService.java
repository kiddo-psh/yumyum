package com.ssafy.manager.auth.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberOAuthService {

    private final MemberRepository memberRepository;

    public Member getOrRegister(String oauthProvider, String oauthId, String email, String nickname) {
        return memberRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId)
                .orElseGet(() -> memberRepository.save(new Member(oauthProvider, oauthId, email, nickname)));
    }
}
