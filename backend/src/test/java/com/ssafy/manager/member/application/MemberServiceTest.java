package com.ssafy.manager.member.application;

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

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberRepository memberRepository;

    @InjectMocks MemberService memberService;

    private static final Long MEMBER_ID = 1L;
    private static final OnboardingCommand COMMAND = new OnboardingCommand(
            Sex.MALE, 1990, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE, HealthGoal.DIET
    );

    @Test
    void 온보딩_완료시_프로필과_플래그가_갱신된다() {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        Member result = memberService.completeOnboarding(MEMBER_ID, COMMAND);

        assertThat(result.isOnboardingCompleted()).isTrue();
        assertThat(result.getSex()).isEqualTo(Sex.MALE);
        assertThat(result.getBirthYear()).isEqualTo(1990);
        assertThat(result.getHeightCm()).isEqualTo(175.0);
        assertThat(result.getWeightKg()).isEqualTo(80.0);
        assertThat(result.getActivityLevel()).isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
        assertThat(result.getHealthGoal()).isEqualTo(HealthGoal.DIET);
    }

    @Test
    void 존재하지_않는_회원이면_예외가_발생한다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.completeOnboarding(MEMBER_ID, COMMAND))
                .isInstanceOf(NoSuchElementException.class);
    }
}
