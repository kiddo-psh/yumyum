package com.ssafy.manager.nyam.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nyam.domain.BodyCategoryResolver;
import com.ssafy.manager.nyam.domain.BodyEvaluationContext;
import com.ssafy.manager.nyam.domain.NyamBodyState;
import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

/**
 * 조회 시점에 NyamBodyState(가상 체중) + 운동 실적을 모아 룰 체인으로 체형 카테고리를 판정한다.
 * BODYBUILDER는 저장하지 않고 트레일링 2주 운동 실적으로 여기서 즉석 평가한다.
 */
@Service
@RequiredArgsConstructor
public class NyamBodyQueryService {

    private final NyamBodyStateManager stateManager;
    private final MemberRepository memberRepository;
    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;

    private final BodyCategoryResolver resolver = BodyCategoryResolver.withDefaultRules();

    @Transactional
    public NyamBodyResult getBody(Long memberId, LocalDate today) {
        NyamBodyState state = stateManager.loadOrCreate(memberId, today);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        int daysPerWeekTarget = routineRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .map(Routine::getDaysPerWeek)
                .orElse(0);
        int recentWeekWorkoutDays = routineSessionRepository
                .countDistinctSessionDatesByMemberIdAndDateBetween(memberId, today.minusDays(6), today);
        int priorWeekWorkoutDays = routineSessionRepository
                .countDistinctSessionDatesByMemberIdAndDateBetween(memberId, today.minusDays(13), today.minusDays(7));

        BodyEvaluationContext context = new BodyEvaluationContext(
                state.virtualWeightKg(),
                member.getHeightCm(),
                daysPerWeekTarget,
                recentWeekWorkoutDays,
                priorWeekWorkoutDays);
        return NyamBodyResult.from(resolver.resolve(context));
    }
}
