package com.ssafy.manager.nyam.domain;

import java.util.Optional;

/**
 * 가상 체중 + 키로 BMI를 계산해 체형 카테고리를 판정한다.
 * 항상 판정되는 terminal 룰(체인 마지막에 위치).
 */
public class BmiRule implements BodyCategoryRule {

    @Override
    public Optional<BodyCategory> evaluate(BodyEvaluationContext context) {
        double heightM = context.heightCm() / 100.0;
        double bmi = context.virtualWeightKg() / (heightM * heightM);
        return Optional.of(BodyCategory.fromBmi(bmi));
    }
}
