package com.ssafy.manager.nyam.domain;

import java.util.List;

/**
 * 우선순위 순서대로 룰을 평가해 첫 번째로 판정된 카테고리를 반환한다.
 * 기본 순서: BodybuilderRule(덮어쓰기) → BmiRule(terminal).
 */
public class BodyCategoryResolver {

    private final List<BodyCategoryRule> rules;

    public BodyCategoryResolver(List<BodyCategoryRule> rules) {
        this.rules = rules;
    }

    public static BodyCategoryResolver withDefaultRules() {
        return new BodyCategoryResolver(List.of(new BodybuilderRule(), new BmiRule()));
    }

    public BodyCategory resolve(BodyEvaluationContext context) {
        return rules.stream()
                .map(rule -> rule.evaluate(context))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("최소 한 룰이 카테고리를 판정해야 합니다."));
    }
}
