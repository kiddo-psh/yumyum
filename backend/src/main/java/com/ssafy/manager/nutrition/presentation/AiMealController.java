package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.AiMealService;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/ai/meals")
@RequiredArgsConstructor
public class AiMealController {

    private final AiMealService aiMealService;

    @PostMapping("/last-recommend")
    public ResponseEntity<LastMealRecommendResponse> lastRecommend(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(LastMealRecommendResponse.from(aiMealService.lastRecommend(memberId, LocalDate.now())));
    }
}
