package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.AiMealService;
import com.ssafy.manager.nutrition.presentation.dto.ChatRequest;
import com.ssafy.manager.nutrition.presentation.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiMealService aiMealService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ChatRequest request
    ) {
        return ResponseEntity.ok(ChatResponse.from(aiMealService.chat(request.message())));
    }
}
