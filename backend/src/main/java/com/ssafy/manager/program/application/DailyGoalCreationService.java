package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
                dailyGoalRepository.save(DailyGoal.of(p.getMemberId(), today, p.getTargetCalories()));
            }
        });
    }
}
