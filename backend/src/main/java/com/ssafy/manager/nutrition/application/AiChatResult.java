package com.ssafy.manager.nutrition.application;

import java.util.List;

public record AiChatResult(String answer, List<Source> sources) {
    public record Source(String name, String info) {}
}
