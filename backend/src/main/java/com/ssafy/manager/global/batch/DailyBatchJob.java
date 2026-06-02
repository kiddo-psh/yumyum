package com.ssafy.manager.global.batch;

import com.ssafy.manager.growth.application.StreakResetService;
import com.ssafy.manager.program.application.DailyGoalCreationService;
import com.ssafy.manager.program.application.ProgramCompletionService;
import com.ssafy.manager.program.application.WeeklyReportService;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyBatchJob {

    private final ProgramCompletionService programCompletionService;
    private final StreakResetService streakResetService;
    private final DailyGoalCreationService dailyGoalCreationService;
    private final WeeklyReportService weeklyReportService;
    private final DailyGoalRepository dailyGoalRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        run(LocalDate.now());
    }

    void run(LocalDate today) {
        programCompletionService.completeExpired(today);

        List<Long> unachievedMemberIds = dailyGoalRepository.findAllByDate(today.minusDays(1))
                .stream()
                .filter(g -> !g.isAchieved())
                .map(g -> g.getMemberId())
                .toList();
        streakResetService.resetFor(unachievedMemberIds);

        dailyGoalCreationService.createForActivePrograms(today);
        weeklyReportService.createStubs(today);
    }
}
