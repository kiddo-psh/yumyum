package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 회원의 뱃지 컬렉션(G602) 조회 결과. 전체 Badge 카탈로그에 회원의 획득 현황을 매핑한 것.
 *
 * <p>미획득 뱃지도 포함된다(조건 공개) — {@code earned=false}, {@code earnedAt=null}.
 */
public record BadgeCollectionResult(List<Item> items) {

    /**
     * 전체 {@link Badge} 카탈로그를 회원의 획득 시각 맵과 left-join 해 Item 목록을 만든다.
     * 맵에 없는 뱃지는 미획득(earned=false, earnedAt=null)으로 포함된다.
     */
    public static BadgeCollectionResult of(Map<Badge, LocalDateTime> earnedAtByBadge) {
        List<Item> items = Arrays.stream(Badge.values())
                .map(badge -> new Item(
                        badge,
                        earnedAtByBadge.containsKey(badge),
                        earnedAtByBadge.get(badge)))
                .toList();
        return new BadgeCollectionResult(items);
    }

    public record Item(Badge badge, boolean earned, LocalDateTime earnedAt) {
    }
}
