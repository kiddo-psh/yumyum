package com.ssafy.manager.growth.domain;

import lombok.Getter;

/**
 * 뱃지 카탈로그 정의. 종류당 하나만 존재하는 전역 마스터 데이터다 (docs/CONTEXT.md).
 *
 * <p>표시 메타데이터(이름·아이콘·설명)와 카테고리를 enum 필드로 가진다.
 * 회원의 획득 사실은 {@link MemberBadge}로 표현된다.
 */
@Getter
public enum Badge {

    ALL_RIGHT("올라잇!!!🔥🔥🔥", "🔥", "운동 세션을 100회 수행했어요", BadgeCategory.WORKOUT),
    WEEKEND_WARRIOR("주말 전사", "🏋️", "주말에 운동 세션을 10회 수행했어요", BadgeCategory.WORKOUT),
    VEGGIE_MANIA("채소매니아", "🥬", "식이섬유가 풍부한 식사를 20회 기록했어요", BadgeCategory.DIET),
    CHICKEN_BREAST_EVANGELIST("닭가슴살 전도사", "🍗", "닭가슴살이 들어간 식사를 30회 기록했어요", BadgeCategory.DIET),
    NOVICE_TAMER("초보 냠냠이 조련사", "🐣", "3일 연속 목표를 달성했어요", BadgeCategory.STREAK),
    LEGENDARY_TAMER("전설의 냠냠이 조련사", "👑", "30일 연속 목표를 달성했어요", BadgeCategory.STREAK);

    private final String displayName;
    private final String icon;
    private final String description;
    private final BadgeCategory category;

    Badge(String displayName, String icon, String description, BadgeCategory category) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.category = category;
    }
}
