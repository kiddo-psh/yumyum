package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.domain.MealType;

import java.time.LocalDate;
import java.util.List;

public record MealResponse(
        Long id,
        MealType type,
        LocalDate date,
        List<MealItemResponse> items,
        List<EarnedBadgeResponse> newlyEarnedBadges
) {
    public record EarnedBadgeResponse(
            String code,
            String name,
            String icon,
            String description
    ) {
        public static EarnedBadgeResponse from(Badge badge) {
            return new EarnedBadgeResponse(
                    badge.name(), badge.getDisplayName(), badge.getIcon(), badge.getDescription());
        }
    }

    public static MealResponse from(Meal meal) {
        return from(meal, List.of());
    }

    public static MealResponse from(Meal meal, List<Badge> earnedBadges) {
        return new MealResponse(
                meal.getId(),
                meal.getType(),
                meal.getDate(),
                meal.getItems().stream().map(MealItemResponse::from).toList(),
                earnedBadges.stream().map(EarnedBadgeResponse::from).toList()
        );
    }
}
