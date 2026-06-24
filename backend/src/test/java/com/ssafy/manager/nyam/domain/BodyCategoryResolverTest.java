package com.ssafy.manager.nyam.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyCategoryResolverTest {

    private final BodyCategoryResolver resolver = BodyCategoryResolver.withDefaultRules();

    @Test
    void 트레일링_2주_목표_달성시_BMI와_무관하게_BODYBUILDER로_덮어쓴다() {
        // 키 170 / 가상체중 80 → BMI 27.7 (비만)이지만 운동 실적이 조건 충족
        BodyEvaluationContext context = new BodyEvaluationContext(80.0, 170.0, 3, 3, 3);

        assertThat(resolver.resolve(context)).isEqualTo(BodyCategory.BODYBUILDER);
    }

    @Test
    void 직전_주만_달성하고_그_이전_주는_미달이면_BMI_카테고리로_떨어진다() {
        BodyEvaluationContext context = new BodyEvaluationContext(80.0, 170.0, 3, 3, 2);

        assertThat(resolver.resolve(context)).isEqualTo(BodyCategory.OBESE);
    }

    @Test
    void 루틴_목표가_없으면_BMI_카테고리로_판정한다() {
        // 키 170 / 가상체중 60 → BMI 20.8 (정상)
        BodyEvaluationContext context = new BodyEvaluationContext(60.0, 170.0, 0, 0, 0);

        assertThat(resolver.resolve(context)).isEqualTo(BodyCategory.NORMAL);
    }
}
