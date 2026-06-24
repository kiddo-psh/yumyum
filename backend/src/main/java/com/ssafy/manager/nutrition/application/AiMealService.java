package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.infrastructure.client.AiChatClientRequest;
import com.ssafy.manager.nutrition.infrastructure.client.AiChatClientResponse;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealClient;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientRequest;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientResponse;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealPhotoClientRequest;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealPhotoClientResponse;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AiMealService {

    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final ProgramRepository programRepository;
    private final AiMealClient aiMealClient;

    @Transactional(readOnly = true)
    public AiMealLastRecommendResult lastRecommend(Long memberId, LocalDate effectiveDate) {
        Program program = programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("활성 프로그램이 없습니다."));

        int mealCount = mealRepository.countByMemberIdAndEffectiveDate(memberId, effectiveDate);
        if (mealCount == 0) {
            throw new IllegalStateException("오늘 식사 기록이 없어 추천을 생성할 수 없습니다.");
        }

        double totalKcal    = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalProtein = mealItemRepository.sumProteinByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalCarb    = mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalFat     = mealItemRepository.sumFatByMemberIdAndEffectiveDate(memberId, effectiveDate);
        if (totalKcal == 0.0 && totalProtein == 0.0 && totalCarb == 0.0 && totalFat == 0.0) {
            throw new IllegalStateException("오늘 식단 내 음식 정보가 없어 추천을 생성할 수 없습니다.");
        }

        AiMealLastRecommendClientResponse resp = aiMealClient.lastRecommend(
                new AiMealLastRecommendClientRequest(
                        totalKcal, totalProtein, totalCarb, totalFat,
                        program.getTargetCalories(), program.getTargetProteinG(),
                        program.getTargetCarbG(), program.getTargetFatG(),
                        mealCount
                )
        );
        List<AiMealLastRecommendResult.Recommendation> recs = resp.recommendations().stream()
                .map(r -> new AiMealLastRecommendResult.Recommendation(
                        r.name(), r.kcal(), r.proteinG(), r.carbG(), r.fatG(), r.reason()))
                .toList();
        return new AiMealLastRecommendResult(recs, resp.priorityNutrient(), resp.aiComment());
    }

    public AiChatResult chat(String message) {
        AiChatClientResponse resp = aiMealClient.chat(new AiChatClientRequest(message, null));
        List<AiChatResult.Source> sources = resp.sources().stream()
                .map(s -> new AiChatResult.Source(s.name(), s.info()))
                .toList();
        return new AiChatResult(resp.answer(), sources);
    }

    public AiMealPhotoAnalyzeResult analyzePhoto(String imageBase64, String mediaType, String mealType) {
        AiMealPhotoClientRequest request = new AiMealPhotoClientRequest(imageBase64, mediaType, mealType);
        AiMealPhotoClientResponse response = aiMealClient.analyzePhoto(request);

        List<AiMealPhotoAnalyzeResult.DetectedItemResult> items = response.detectedItems().stream()
                .map(d -> new AiMealPhotoAnalyzeResult.DetectedItemResult(
                        d.name(), d.estimatedGrams(), d.kcal(), d.proteinG(), d.carbG(), d.fatG()))
                .toList();

        return new AiMealPhotoAnalyzeResult(items, response.totalKcal(), response.aiComment());
    }
}
