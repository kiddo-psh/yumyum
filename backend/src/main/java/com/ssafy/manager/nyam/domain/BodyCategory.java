package com.ssafy.manager.nyam.domain;

/**
 * Nyam 체형 카테고리.
 * UNDERWEIGHT~OBESE는 가상 BMI(대한비만학회 / WHO 아시아-태평양 기준)로 산출하고,
 * BODYBUILDER는 운동 수행 실적에 따라 조회 시 덮어쓰기로 적용한다.
 */
public enum BodyCategory {

    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE,
    BODYBUILDER;

    public static BodyCategory fromBmi(double bmi) {
        if (bmi < 18.5) return UNDERWEIGHT;
        if (bmi < 23) return NORMAL;
        if (bmi < 25) return OVERWEIGHT;
        return OBESE;
    }
}
