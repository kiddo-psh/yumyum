package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.MealItem;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealClient;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientRequest;
import com.ssafy.manager.nutrition.infrastructure.client.AiMealLastRecommendClientResponse;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nutrition.presentation.dto.LastMealRecommendResponse;
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
    public LastMealRecommendResponse lastRecommend(Long memberId, LocalDate effectiveDate) {
        Program program = programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("활성 프로그램이 없습니다."));

        List<MealItem> items = mealItemRepository.findAllByMemberIdAndEffectiveDate(memberId, effectiveDate);
        double totalKcal    = items.stream().mapToDouble(MealItem::getCalories).sum();
        double totalProtein = items.stream().mapToDouble(MealItem::getProtein).sum();
        double totalCarb    = items.stream().mapToDouble(MealItem::getCarbs).sum();
        double totalFat     = items.stream().mapToDouble(MealItem::getFat).sum();
        int mealCount = mealRepository.countByMemberIdAndEffectiveDate(memberId, effectiveDate);

        AiMealLastRecommendClientResponse resp = aiMealClient.lastRecommend(
                new AiMealLastRecommendClientRequest(
                        totalKcal, totalProtein, totalCarb, totalFat,
                        program.getTargetCalories(), program.getTargetProteinG(),
                        program.getTargetCarbG(), program.getTargetFatG(),
                        mealCount
                )
        );
        return LastMealRecommendResponse.from(resp);
    }
}
