package com.ssafy.manager.program.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiCoachingClient {

    private final RestClient aiCoachingRestClient;

    public AiCoachingClientResponse weekly(AiCoachingClientRequest request) {
        return aiCoachingRestClient.post()
                .uri("/ai/coaching/weekly")
                .body(request)
                .retrieve()
                .body(AiCoachingClientResponse.class);
    }
}
