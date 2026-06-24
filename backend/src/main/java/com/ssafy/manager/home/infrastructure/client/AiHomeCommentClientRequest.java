package com.ssafy.manager.home.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiHomeCommentClientRequest(
        @JsonProperty("member_id") Long memberId,
        @JsonProperty("health_goal") String healthGoal,
        @JsonProperty("current_streak") int currentStreak,
        @JsonProperty("kcal_rate") double kcalRate,
        @JsonProperty("remaining_kcal") double remainingKcal,
        @JsonProperty("protein_g") double proteinG,
        @JsonProperty("carb_g") double carbG,
        @JsonProperty("fat_g") double fatG
) {}
