package com.ssafy.manager.member.application;

import com.ssafy.manager.member.application.dto.OnboardingResult;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public OnboardingResult completeOnboarding(Long memberId, OnboardingCommand command) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        member.completeOnboarding(
                command.sex(),
                command.birthYear(),
                command.heightCm(),
                command.weightKg(),
                command.activityLevel(),
                command.healthGoal()
        );
        return OnboardingResult.from(member);
    }
}
