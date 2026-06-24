package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiChatClientResponse(
        @JsonProperty("answer")  String answer,
        @JsonProperty("sources") List<Source> sources
) {
    public record Source(
            @JsonProperty("name") String name,
            @JsonProperty("info") String info
    ) {}
}
