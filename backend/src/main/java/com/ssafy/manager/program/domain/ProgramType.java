package com.ssafy.manager.program.domain;

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

    public int adjust(int tdee) {
        return tdee + adjustment;
    }
}
