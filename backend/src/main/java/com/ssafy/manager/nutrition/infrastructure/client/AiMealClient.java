package com.ssafy.manager.nutrition.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiMealClient {

    private final RestClient aiMealRestClient;

    public AiMealLastRecommendClientResponse lastRecommend(AiMealLastRecommendClientRequest request) {
        return aiMealRestClient.post()
                .uri("/ai/meal/last-recommend")
                .body(request)
                .retrieve()
                .body(AiMealLastRecommendClientResponse.class);
    }
}
