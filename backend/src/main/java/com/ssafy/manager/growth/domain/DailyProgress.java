package com.ssafy.manager.growth.domain;

import com.ssafy.manager.program.domain.DailyGoal;

public record DailyProgress(
        double targetKcal,
        double achievedKcal,
        double achievementRate,
        boolean achieved
) {
    public static DailyProgress from(DailyGoal goal) {
        double rate = goal.getAchievedValue() / goal.getTargetValue();
        return new DailyProgress(goal.getTargetValue(), goal.getAchievedValue(), rate, goal.isAchieved());
    }

    public static DailyProgress empty() {
        return new DailyProgress(0.0, 0.0, 0.0, false);
    }
}
