package com.ssafy.manager.nyam.domain;

import java.util.Optional;

/**
 * 트레일링 2주(직전 7일 / 그 이전 7일) 두 구간이 모두 주간 목표(daysPerWeek) 이상
 * 운동일을 채웠으면 BODYBUILDER로 판정한다. 그 외에는 다음 룰로 넘긴다.
 */
public class BodybuilderRule implements BodyCategoryRule {

    @Override
    public Optional<BodyCategory> evaluate(BodyEvaluationContext context) {
        int target = context.daysPerWeekTarget();
        if (target <= 0) {
            return Optional.empty();
        }
        boolean qualified = context.recentWeekWorkoutDays() >= target
                && context.priorWeekWorkoutDays() >= target;
        return qualified ? Optional.of(BodyCategory.BODYBUILDER) : Optional.empty();
    }
}
