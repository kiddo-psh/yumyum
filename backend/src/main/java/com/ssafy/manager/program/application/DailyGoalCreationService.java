package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyGoalCreationService {

    private final ProgramRepository programRepository;
    private final DailyGoalRepository dailyGoalRepository;

    @Transactional
    public void createForActivePrograms(LocalDate today) {
        programRepository.findAllByStatus(ProgramStatus.ACTIVE).forEach(p -> {
            if (!dailyGoalRepository.existsByMemberIdAndDate(p.getMemberId(), today)) {
                dailyGoalRepository.save(DailyGoal.of(
                        p.getMemberId(), today,
                        p.getTargetCalories(),
                        p.getTargetProteinG(),
                        p.getTargetCarbG(),
                        p.getTargetFatG()));
            }
        });
    }

    /**
     * 배치가 아직 안 돌아 당일 DailyGoal이 없을 때 온디맨드로 생성.
     * readOnly 트랜잭션 밖에서도 커밋되도록 REQUIRES_NEW 사용.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureGoalExists(Long memberId, LocalDate date) {
        if (dailyGoalRepository.existsByMemberIdAndDate(memberId, date)) {
            return;
        }
        programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE).ifPresent(p ->
                dailyGoalRepository.save(DailyGoal.of(
                        memberId, date,
                        p.getTargetCalories(),
                        p.getTargetProteinG(),
                        p.getTargetCarbG(),
                        p.getTargetFatG()))
        );
    }
}
