package com.ssafy.manager.global.batch;

import com.ssafy.manager.growth.application.StreakResetService;
import com.ssafy.manager.program.application.DailyGoalCreationService;
import com.ssafy.manager.program.application.ProgramCompletionService;
import com.ssafy.manager.program.application.WeeklyReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class DailyBatchJobTest {

    @Mock ProgramCompletionService programCompletionService;
    @Mock StreakResetService streakResetService;
    @Mock DailyGoalCreationService dailyGoalCreationService;
    @Mock WeeklyReportService weeklyReportService;

    @InjectMocks DailyBatchJob job;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void 배치_스텝이_정해진_순서대로_실행된다() {
        job.run(TODAY);

        InOrder order = inOrder(programCompletionService, streakResetService,
                dailyGoalCreationService, weeklyReportService);
        order.verify(programCompletionService).completeExpired(TODAY);
        order.verify(streakResetService).resetUnachievedFor(TODAY);
        order.verify(dailyGoalCreationService).createForActivePrograms(TODAY);
        order.verify(weeklyReportService).createStubs(TODAY);
    }
}
