package com.ssafy.manager.program.domain;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.Sex;

public class TdeeCalculator {

    private TdeeCalculator() {}

    public static int calculate(Sex sex, int age, double heightCm, double weightKg, ActivityLevel activityLevel) {
        double bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + (sex == Sex.MALE ? 5 : -161);
        return (int) Math.round(bmr * activityLevel.getMultiplier());
    }
}
