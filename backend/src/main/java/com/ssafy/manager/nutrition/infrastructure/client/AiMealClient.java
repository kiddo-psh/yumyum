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

    public AiMealPhotoClientResponse analyzePhoto(AiMealPhotoClientRequest request) {
        return aiMealRestClient.post()
                .uri("/ai/meal/analyze-photo")
                .body(request)
                .retrieve()
                .body(AiMealPhotoClientResponse.class);
    }

    public AiChatClientResponse chat(AiChatClientRequest request) {
        return aiMealRestClient.post()
                .uri("/ai/chat")
                .body(request)
                .retrieve()
                .body(AiChatClientResponse.class);
    }
}
