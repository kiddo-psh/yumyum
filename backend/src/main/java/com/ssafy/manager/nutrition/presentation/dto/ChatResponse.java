package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.AiChatResult;

import java.util.List;

public record ChatResponse(String answer, List<Source> sources) {

    public record Source(String name, String info) {}

    public static ChatResponse from(AiChatResult result) {
        List<Source> sources = result.sources().stream()
                .map(s -> new Source(s.name(), s.info()))
                .toList();
        return new ChatResponse(result.answer(), sources);
    }
}
