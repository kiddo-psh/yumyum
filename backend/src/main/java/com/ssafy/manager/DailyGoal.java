package com.ssafy.manager;

import lombok.Getter;

@Getter
public class DailyGoal {
    private final double targetValue;
    private double achievedValue = 0;
    private boolean achieved = false;

    public DailyGoal(double targetValue) {
        this.targetValue = targetValue;
    }

    public void recalculate(double value) {
        achievedValue = value;
        if (!achieved) {
            achieved = targetValue <= achievedValue;
        }
    }
}
