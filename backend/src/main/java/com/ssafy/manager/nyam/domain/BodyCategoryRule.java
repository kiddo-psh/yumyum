package com.ssafy.manager.nyam.domain;

import java.util.Optional;

/**
 * 체형 카테고리 판정 룰(Chain of Responsibility).
 * 판정되면 카테고리를 반환하고 이후 룰은 평가하지 않는다(덮어쓰기 + 종료).
 * 판정 불가면 empty를 반환해 다음 룰로 넘긴다.
 */
public interface BodyCategoryRule {

    Optional<BodyCategory> evaluate(BodyEvaluationContext context);
}
