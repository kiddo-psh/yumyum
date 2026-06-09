package com.ssafy.manager.program.infrastructure.client;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.program.domain.ProgramType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AiPlanClient {

    private final RestClient aiPlanRestClient;

    public AiPlanClientResponse generate(Member member, ProgramType type) {
        AiPlanClientRequest request = buildRequest(member, type);
        return aiPlanRestClient.post()
                .uri("/ai/plan/generate")
                .body(request)
                .retrieve()
                .body(AiPlanClientResponse.class);
    }

    private AiPlanClientRequest buildRequest(Member member, ProgramType type) {
        int age = member.age(LocalDate.now().getYear());
        return new AiPlanClientRequest(
                member.getSex() == Sex.MALE ? "M" : "F",
                age,
                member.getHeightCm(),
                member.getWeightKg(),
                toActivityLevel(member.getActivityLevel()),
                toHealthGoal(type)
        );
    }

    private String toActivityLevel(ActivityLevel level) {
        return switch (level) {
            case SEDENTARY -> "sedentary";
            case LIGHTLY_ACTIVE -> "light";
            case MODERATELY_ACTIVE -> "moderate";
            case VERY_ACTIVE -> "active";
            case EXTRA_ACTIVE -> "very_active";
        };
    }

    private String toHealthGoal(ProgramType type) {
        return switch (type) {
            case DIET -> "WEIGHT_LOSS";
            case MUSCLE -> "MUSCLE_GAIN";
            case HEALTH, DISEASE -> "MAINTAIN";
        };
    }
}
