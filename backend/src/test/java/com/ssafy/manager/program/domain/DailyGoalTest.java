package com.ssafy.manager.program.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DailyGoalTest {

    @Test
    void 목표_기록보다_작으면_미달성() {
        DailyGoal goal = new DailyGoal(2000.0);
        goal.recalculate(1999.0);
        assertFalse(goal.isAchieved());
    }

    @Test
    void 목표_기록보다_이상이면_달성() {
        DailyGoal goal = new DailyGoal(2000.0);
        goal.recalculate(2000.0);

        assertTrue(goal.isAchieved());
    }

    @Test
    void 달성_후_달성값이_낮아져도_달성_상태는_유지된다() {
        DailyGoal goal = new DailyGoal(2000.0);
        goal.recalculate(2000.0);

        goal.recalculate(-1000.0);

        assertTrue(goal.isAchieved());
    }

}
