package com.ssafy.manager.global.batch;

import com.ssafy.manager.growth.application.StreakResetService;
import com.ssafy.manager.nyam.application.NyamBodyDailyUpdateService;
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
    private final NyamBodyDailyUpdateService nyamBodyDailyUpdateService;
    private final WeeklyReportService weeklyReportService;

    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        run(LocalDate.now());
    }

    // 순서 중요: 만료 완료 → 전날 Streak 리셋 → 전날 Nyam 체형 누적 → WeeklyReport stub 생성
    // DailyGoal은 API 호출 시 lazy 생성 (ensureGoalExists) — 배치 선행 생성 불필요
    void run(LocalDate today) {
        programCompletionService.completeExpired(today);
        streakResetService.resetUnachievedFor(today);
        nyamBodyDailyUpdateService.updateFor(today);
        weeklyReportService.createStubs(today);
    }
}
