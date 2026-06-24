package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원의 뱃지 컬렉션(G602) 조회 결과. 전체 Badge 카탈로그에 회원의 획득 현황을 매핑한 것.
 *
 * <p>미획득 뱃지도 포함된다(조건 공개) — {@code earned=false}, {@code earnedAt=null}.
 */
public record BadgeCollectionResult(List<Item> items) {

    public record Item(Badge badge, boolean earned, LocalDateTime earnedAt) {
    }
}
