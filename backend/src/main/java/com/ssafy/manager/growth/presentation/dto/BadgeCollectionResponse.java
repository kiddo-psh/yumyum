package com.ssafy.manager.growth.presentation.dto;

import com.ssafy.manager.growth.application.BadgeCollectionResult;
import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.growth.domain.BadgeCategory;

import java.time.LocalDateTime;
import java.util.List;

public record BadgeCollectionResponse(List<BadgeItem> badges) {

    public record BadgeItem(
            String code,
            String name,
            String icon,
            String description,
            BadgeCategory category,
            boolean earned,
            LocalDateTime earnedAt
    ) {
        public static BadgeItem from(BadgeCollectionResult.Item item) {
            Badge badge = item.badge();
            return new BadgeItem(
                    badge.name(),
                    badge.getDisplayName(),
                    badge.getIcon(),
                    badge.getDescription(),
                    badge.getCategory(),
                    item.earned(),
                    item.earnedAt());
        }
    }

    public static BadgeCollectionResponse from(BadgeCollectionResult result) {
        return new BadgeCollectionResponse(
                result.items().stream().map(BadgeItem::from).toList());
    }
}
