package com.ssafy.manager.member.application;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Sex;

public record OnboardingCommand(
        Sex sex,
        int birthYear,
        double heightCm,
        double weightKg,
        ActivityLevel activityLevel,
        HealthGoal healthGoal
) {}
