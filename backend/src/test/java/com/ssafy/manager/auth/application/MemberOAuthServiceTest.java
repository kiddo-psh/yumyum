package com.ssafy.manager.auth.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberOAuthServiceTest {

    @Mock MemberRepository memberRepository;

    @InjectMocks MemberOAuthService memberOAuthService;

    @Test
    void 신규_OAuth_유저는_Member가_생성된다() {
        given(memberRepository.findByOauthProviderAndOauthId("kakao", "12345")).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        Member result = memberOAuthService.getOrRegister(new OAuthUserInfo("kakao", "12345", "test@kakao.com"));

        verify(memberRepository).save(any(Member.class));
        assertThat(result.getOauthProvider()).isEqualTo("kakao");
        assertThat(result.getOauthId()).isEqualTo("12345");
        assertThat(result.getEmail()).isEqualTo("test@kakao.com");
        assertThat(result.isOnboardingCompleted()).isFalse();
    }

    @Test
    void 기존_OAuth_유저는_Member가_중복_생성되지_않는다() {
        Member existing = new Member("kakao", "12345", "test@kakao.com");
        given(memberRepository.findByOauthProviderAndOauthId("kakao", "12345")).willReturn(Optional.of(existing));

        Member result = memberOAuthService.getOrRegister(new OAuthUserInfo("kakao", "12345", "test@kakao.com"));

        verify(memberRepository, never()).save(any(Member.class));
        assertThat(result).isSameAs(existing);
    }
}
