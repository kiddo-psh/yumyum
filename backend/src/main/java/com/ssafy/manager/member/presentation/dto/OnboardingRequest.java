package com.ssafy.manager.member.presentation.dto;

import com.ssafy.manager.member.application.OnboardingCommand;
import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Sex;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record OnboardingRequest(
        Sex sex,
        @Min(value = 1800, message = "출생연도은 1800 이상 9999 이하 여야 합니다.")
        @Max(value = 9999, message = "출생연도는 1800 이상 9999 이하 여야 합니다.") int birthYear,
        @Positive(message = "키는 0보다 커야 합니다.") double heightCm,
        @Positive(message = "체중은 0보다 커야합니다.") double weightKg,
        ActivityLevel activityLevel,
        HealthGoal healthGoal
) {
    public OnboardingCommand toCommand() {
        return new OnboardingCommand(sex, birthYear, heightCm, weightKg, activityLevel, healthGoal);
    }
}
