package com.ssafy.manager.global.batch;

import com.ssafy.manager.growth.application.StreakResetService;
import com.ssafy.manager.program.application.DailyGoalCreationService;
import com.ssafy.manager.program.application.ProgramCompletionService;
import com.ssafy.manager.program.application.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DailyBatchJob {

    private final ProgramCompletionService programCompletionService;
    private final StreakResetService streakResetService;
    private final DailyGoalCreationService dailyGoalCreationService;
    private final WeeklyReportService weeklyReportService;

    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        run(LocalDate.now());
    }

    void run(LocalDate today) {
        programCompletionService.completeExpired(today);
        streakResetService.resetUnachievedFor(today.minusDays(1));
        dailyGoalCreationService.createForActivePrograms(today);
        weeklyReportService.createStubs(today);
    }
}
