package com.ssafy.manager.growth.domain;

public enum DailyAchievementStatus {
    ACHIEVED, PARTIAL, NONE;

    public static DailyAchievementStatus of(double rate) {
        if (rate >= 1.0) return ACHIEVED;
        if (rate > 0.0) return PARTIAL;
        return NONE;
    }
}
