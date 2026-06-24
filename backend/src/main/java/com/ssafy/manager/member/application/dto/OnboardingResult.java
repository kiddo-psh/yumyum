package com.ssafy.manager.member.application.dto;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;

public record OnboardingResult(
        Long memberId,
        boolean onboardingCompleted,
        Sex sex,
        Integer birthYear,
        Double heightCm,
        Double weightKg,
        ActivityLevel activityLevel,
        HealthGoal healthGoal
) {
    public static OnboardingResult from(Member member) {
        return new OnboardingResult(
                member.getId(),
                member.isOnboardingCompleted(),
                member.getSex(),
                member.getBirthYear(),
                member.getHeightCm(),
                member.getWeightKg(),
                member.getActivityLevel(),
                member.getHealthGoal()
        );
    }
}
