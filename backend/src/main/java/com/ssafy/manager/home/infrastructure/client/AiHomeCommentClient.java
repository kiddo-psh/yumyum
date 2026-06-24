package com.ssafy.manager.home.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiHomeCommentClient {

    private final RestClient aiHomeCommentRestClient;

    public AiHomeCommentClientResponse request(AiHomeCommentClientRequest req) {
        return aiHomeCommentRestClient.post()
                .uri("/ai/home/comment")
                .body(req)
                .retrieve()
                .body(AiHomeCommentClientResponse.class);
    }
}
