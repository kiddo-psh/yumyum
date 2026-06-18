package com.ssafy.manager.member.presentation.dto;

import com.ssafy.manager.member.application.OnboardingCommand;
import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Sex;

public record OnboardingRequest(
        Sex sex,
        int birthYear,
        double heightCm,
        double weightKg,
        ActivityLevel activityLevel,
        HealthGoal healthGoal
) {
    public OnboardingCommand toCommand() {
        return new OnboardingCommand(sex, birthYear, heightCm, weightKg, activityLevel, healthGoal);
    }
}
