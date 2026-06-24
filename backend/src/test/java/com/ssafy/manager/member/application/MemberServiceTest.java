package com.ssafy.manager.member.application;

import com.ssafy.manager.member.application.dto.OnboardingResult;
import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MemberService memberService;

    private static final Long MEMBER_ID = 1L;
    private static final OnboardingCommand COMMAND = new OnboardingCommand(
            Sex.MALE, 1990, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE, HealthGoal.DIET
    );

    @Test
    void 온보딩_완료시_프로필과_플래그가_갱신된다() {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        OnboardingResult result = memberService.completeOnboarding(MEMBER_ID, COMMAND);

        assertThat(result.onboardingCompleted()).isTrue();
        assertThat(result.sex()).isEqualTo(Sex.MALE);
        assertThat(result.birthYear()).isEqualTo(1990);
        assertThat(result.heightCm()).isEqualTo(175.0);
        assertThat(result.weightKg()).isEqualTo(80.0);
        assertThat(result.activityLevel()).isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
        assertThat(result.healthGoal()).isEqualTo(HealthGoal.DIET);
    }

    @Test
    void 온보딩_완료시_MemberOnboardedEvent가_발행된다() {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        memberService.completeOnboarding(MEMBER_ID, COMMAND);

        verify(eventPublisher).publishEvent(new MemberOnboardedEvent(MEMBER_ID));
    }

    @Test
    void 존재하지_않는_회원이면_예외가_발생한다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.completeOnboarding(MEMBER_ID, COMMAND))
                .isInstanceOf(NoSuchElementException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
