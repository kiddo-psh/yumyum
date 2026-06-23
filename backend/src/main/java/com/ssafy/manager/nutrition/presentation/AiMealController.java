package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.AiMealService;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
import com.ssafy.manager.nutrition.presentation.dto.PhotoAnalysisRequest;
import com.ssafy.manager.nutrition.presentation.dto.PhotoAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class AiMealController {

    private final AiMealService aiMealService;

    @PostMapping("/last-recommend")
    public ResponseEntity<LastMealRecommendResponse> lastRecommend(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(LastMealRecommendResponse.from(aiMealService.lastRecommend(memberId, LocalDate.now())));
    }

    @PostMapping("/photo/analyze")
    public ResponseEntity<PhotoAnalysisResponse> analyzePhoto(
            @AuthenticationPrincipal Long memberId,
            @RequestBody PhotoAnalysisRequest request
    ) {
        return ResponseEntity.ok(PhotoAnalysisResponse.from(
                aiMealService.analyzePhoto(request.imageBase64(), request.mediaType(), request.mealType())));
    }
}
