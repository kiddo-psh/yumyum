package com.ssafy.manager.program.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.OnboardingRequiredException;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.client.AiPlanClient;
import com.ssafy.manager.program.infrastructure.client.AiPlanClientResponse;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final MemberRepository memberRepository;
    private final ProgramRepository programRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final AiPlanClient aiPlanClient;

    @Transactional
    public ProgramResult create(Long memberId, LocalDate startDate, int durationWeeks) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (!member.isOnboardingCompleted()) {
            throw new OnboardingRequiredException(
                    "프로필 정보가 없어 프로그램을 생성할 수 없습니다. 온보딩을 먼저 완료해 주세요.");
        }

        programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new IllegalStateException("이미 활성화된 Program이 있습니다."); });

        ProgramType type = ProgramType.from(member.getHealthGoal());
        AiPlanClientResponse aiPlan = aiPlanClient.generate(member, type);

        LocalDate endDate = startDate.plusWeeks(durationWeeks);
        int targetCalories = (int) Math.round(aiPlan.targetKcal());

        Program program = Program.create(
                memberId, type, startDate, endDate, targetCalories,
                aiPlan.targetProteinG(), aiPlan.targetCarbG(), aiPlan.targetFatG(), aiPlan.aiComment()
        );
        programRepository.save(program);

        // 배치(새벽 4시)를 기다리지 않고 오늘 DailyGoal을 즉시 생성
        if (!dailyGoalRepository.existsByMemberIdAndDate(memberId, startDate)) {
            dailyGoalRepository.save(DailyGoal.of(
                    memberId, startDate,
                    targetCalories,
                    aiPlan.targetProteinG(), aiPlan.targetCarbG(), aiPlan.targetFatG()
            ));
        }

        return ProgramResult.from(program, durationWeeks);
    }

    @Transactional(readOnly = true)
    public ProgramResult getCurrent(Long memberId) {
        Program program = programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("활성화된 Program이 없습니다."));

        long days = ChronoUnit.DAYS.between(program.getStartDate(), program.getEndDate());
        int weeks = (int) (days / 7);

        return ProgramResult.from(program, weeks);
    }
}
