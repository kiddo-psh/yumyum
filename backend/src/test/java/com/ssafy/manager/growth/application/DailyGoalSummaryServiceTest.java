package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.WeeklyAchievementSummary;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DailyGoalSummaryServiceTest {

    @Mock
    DailyGoalRepository dailyGoalRepository;

    @InjectMocks
    DailyGoalSummaryService dailyGoalSummaryService;

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 8);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 6, 14);

    @Test
    void 주중_어떤_날짜를_넘겨도_해당_주_DailyGoal_달성률을_포함한_요약을_반환한다() {
        final int targetCal = 2000;
        final int achievedCal = 1000;
        final double achievementRate = (double) achievedCal / targetCal;
        LocalDate wednesday = LocalDate.of(2026, 6, 10);
        DailyGoal mondayGoal = DailyGoal.of(MEMBER_ID, MONDAY, targetCal);
        mondayGoal.recalculate(achievedCal);

        given(dailyGoalRepository.findAllByMemberIdAndDateBetween(MEMBER_ID, MONDAY, SUNDAY))
                .willReturn(List.of(mondayGoal));

        WeeklyAchievementSummary result = dailyGoalSummaryService.weeklyCalendar(MEMBER_ID, wednesday);


        assertThat(result.achievementRateFor(MONDAY)).isEqualTo(achievementRate);
    }
}
