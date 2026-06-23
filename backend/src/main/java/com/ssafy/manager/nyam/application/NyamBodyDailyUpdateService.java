package com.ssafy.manager.nyam.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.nyam.domain.DailyBalanceCalculator;
import com.ssafy.manager.nyam.domain.NyamBodyState;
import com.ssafy.manager.nyam.infrastructure.persistence.NyamBodyStateRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.TdeeCalculator;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 매일 배치: 전날 마감분(섭취 vs TDEE+운동소모)을 각 회원의 NyamBodyState에 누적한다.
 * 상위 도메인의 push 없이, DailyGoal·Meal·RoutineSession·Member를 읽어 스스로 파생한다.
 */
@Service
@RequiredArgsConstructor
public class NyamBodyDailyUpdateService {

    private final DailyGoalRepository dailyGoalRepository;
    private final MealRepository mealRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final MemberRepository memberRepository;
    private final NyamBodyStateRepository nyamBodyStateRepository;
    private final NyamBodyStateManager stateManager;

    @Transactional
    public void updateFor(LocalDate today) {
        LocalDate targetDate = today.minusDays(1);
        List<DailyGoal> goals = dailyGoalRepository.findAllByDate(targetDate);
        for (DailyGoal goal : goals) {
            applyDailyBalance(goal, targetDate, today);
        }
    }

    private void applyDailyBalance(DailyGoal goal, LocalDate targetDate, LocalDate today) {
        Long memberId = goal.getMemberId();
        NyamBodyState state = stateManager.loadOrCreate(memberId, targetDate);
        stateManager.reAnchorToLatestWeight(state);

        if (!targetDate.isAfter(state.getAnchorDate())) {
            return; // anchor 시점 이전/당일은 누적하지 않는다
        }

        double balance = DailyBalanceCalculator.calculate(
                (int) goal.getTargetValue(),
                goal.getAchievedValue(),
                mealRepository.countByMemberIdAndEffectiveDate(memberId, targetDate),
                routineSessionRepository.sumCaloriesBurnedByMemberIdAndDate(memberId, targetDate),
                tdeeOf(memberId, today.getYear()),
                memberId,
                targetDate);

        state.applyDailyBalance(balance);
        nyamBodyStateRepository.save(state);
    }

    private int tdeeOf(Long memberId, int currentYear) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        return TdeeCalculator.calculate(
                member.getSex(),
                member.age(currentYear),
                member.getHeightCm(),
                member.getWeightKg(),
                member.getActivityLevel());
    }
}
