package com.ssafy.manager.auth.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberOAuthService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Member getOrRegister(OAuthUserInfo userInfo) {
        return memberRepository.findByOauthProviderAndOauthId(userInfo.provider(), userInfo.oauthId())
                .orElseGet(() -> register(userInfo));
    }

    private Member register(OAuthUserInfo userInfo) {
        Member member = memberRepository.save(
                new Member(userInfo.provider(), userInfo.oauthId(), userInfo.email()));
        eventPublisher.publishEvent(new MemberRegisteredEvent(member.getId()));
        return member;
    }
}
