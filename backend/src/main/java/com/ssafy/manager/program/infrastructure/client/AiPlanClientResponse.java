package com.ssafy.manager.program.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPlanClientResponse(
        double bmr,
        double tdee,
        @JsonProperty("target_kcal") double targetKcal,
        @JsonProperty("target_protein_g") double targetProteinG,
        @JsonProperty("target_carb_g") double targetCarbG,
        @JsonProperty("target_fat_g") double targetFatG,
        @JsonProperty("ai_comment") String aiComment
) {}
