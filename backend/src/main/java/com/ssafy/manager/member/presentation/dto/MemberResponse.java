package com.ssafy.manager.member.presentation.dto;

import com.ssafy.manager.member.application.dto.OnboardingResult;
import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Sex;

public record MemberResponse(
        Long memberId,
        boolean onboardingCompleted,
        Sex sex,
        Integer birthYear,
        Double heightCm,
        Double weightKg,
        ActivityLevel activityLevel,
        HealthGoal healthGoal
) {
    public static MemberResponse from(OnboardingResult member) {
        return new MemberResponse(
                member.memberId(),
                member.onboardingCompleted(),
                member.sex(),
                member.birthYear(),
                member.heightCm(),
                member.weightKg(),
                member.activityLevel(),
                member.healthGoal()
        );
    }
}
