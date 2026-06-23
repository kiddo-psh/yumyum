package com.ssafy.manager.growth.presentation.dto;

import com.ssafy.manager.growth.domain.NyamMood;
import com.ssafy.manager.member.domain.HealthGoal;

public record NyamStatusResponse(
        NyamMood mood,
        String message,
        double achievementRate,
        HealthGoal healthGoal
) {
    private static final java.util.Map<NyamMood, String> MESSAGES = java.util.Map.of(
            NyamMood.HAPPY,  "오늘도 목표 달성! 냠냠이가 기뻐해요",
            NyamMood.NORMAL, "절반쯤 왔어요. 조금만 더 힘내요!",
            NyamMood.SAD,    "오늘 아직 기록이 없어요. 냠냠이가 기다리고 있어요"
    );

    public static NyamStatusResponse of(double achievementRate, HealthGoal healthGoal) {
        NyamMood mood = NyamMood.from(achievementRate);
        return new NyamStatusResponse(mood, MESSAGES.get(mood), achievementRate, healthGoal);
    }
}
