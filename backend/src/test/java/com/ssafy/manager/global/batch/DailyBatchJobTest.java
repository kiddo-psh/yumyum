package com.ssafy.manager.global.batch;

import com.ssafy.manager.growth.application.StreakResetService;
import com.ssafy.manager.program.application.DailyGoalCreationService;
import com.ssafy.manager.program.application.ProgramCompletionService;
import com.ssafy.manager.program.application.WeeklyReportService;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class DailyBatchJobTest {

    @Mock ProgramCompletionService programCompletionService;
    @Mock StreakResetService streakResetService;
    @Mock DailyGoalCreationService dailyGoalCreationService;
    @Mock WeeklyReportService weeklyReportService;
    @Mock DailyGoalRepository dailyGoalRepository;

    @InjectMocks DailyBatchJob job;

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);

    @Test
    void 배치_스텝이_정해진_순서대로_실행된다() {
        given(dailyGoalRepository.findAllByDate(TODAY.minusDays(1))).willReturn(List.of());

        job.run(TODAY);

        InOrder order = inOrder(programCompletionService, streakResetService,
                dailyGoalCreationService, weeklyReportService);
        order.verify(programCompletionService).completeExpired(TODAY);
        order.verify(streakResetService).resetFor(List.of());
        order.verify(dailyGoalCreationService).createForActivePrograms(TODAY);
        order.verify(weeklyReportService).createStubs(TODAY);
    }

    @Test
    void 미달성_DailyGoal의_memberId만_streak_리셋에_전달된다() {
        DailyGoal unachieved = DailyGoal.of(1L, TODAY.minusDays(1), 2400);
        DailyGoal achieved = DailyGoal.of(2L, TODAY.minusDays(1), 2400);
        achieved.recalculate(2400);

        given(dailyGoalRepository.findAllByDate(TODAY.minusDays(1)))
                .willReturn(List.of(unachieved, achieved));

        job.run(TODAY);

        org.mockito.Mockito.verify(streakResetService).resetFor(List.of(1L));
    }
}
