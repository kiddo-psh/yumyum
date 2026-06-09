package com.ssafy.manager.routine.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiRoutineClient {

    private final RestClient aiRoutineRestClient;

    public AiRoutineClientResponse generate(AiRoutineClientRequest request) {
        return aiRoutineRestClient.post()
                .uri("/ai/routine/generate")
                .body(request)
                .retrieve()
                .body(AiRoutineClientResponse.class);
    }
}
