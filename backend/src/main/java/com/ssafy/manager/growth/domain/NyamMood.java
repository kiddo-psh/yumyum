package com.ssafy.manager.growth.domain;

public enum NyamMood {
    HAPPY, NORMAL, SAD;

    public static NyamMood from(double achievementRate) {
        if (achievementRate >= 0.8) return HAPPY;
        if (achievementRate >= 0.4) return NORMAL;
        return SAD;
    }
}
