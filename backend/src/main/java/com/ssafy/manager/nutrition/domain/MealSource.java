package com.ssafy.manager.nutrition.domain;

/**
 * Meal이 기록된 경로. 누적 통계·뱃지 평가에 사용된다.
 *
 * <p>{@code MANUAL} 직접 입력, {@code PHOTO} 사진 분석 기록, {@code AI} AI 추정 입력.
 */
public enum MealSource {
    MANUAL,
    PHOTO,
    AI
}
