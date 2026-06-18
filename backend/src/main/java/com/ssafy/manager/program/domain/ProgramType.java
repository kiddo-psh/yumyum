package com.ssafy.manager.program.domain;

import com.ssafy.manager.member.domain.HealthGoal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProgramType {
    DIET(-500),
    MUSCLE(300),
    HEALTH(0),
    DISEASE(0);

    private final int adjustment;

    public static ProgramType from(HealthGoal healthGoal) {
        return switch (healthGoal) {
            case DIET -> DIET;
            case MUSCLE -> MUSCLE;
            case HEALTH -> HEALTH;
            case DISEASE -> DISEASE;
        };
    }

    public int adjust(int tdee) {
        return tdee + adjustment;
    }
}
