package com.ssafy.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DailyGoalTest {

    @Test
    void 목표_기록보다_작으면_미달성() {
        DailyGoal goal = new DailyGoal(2);
        goal.progress();
        assertFalse(goal.isAchieved());
    }

    @Test
    void 목표_기록보다_이상이면_달성() {
        DailyGoal goal = new DailyGoal(2);
        goal.progress();
        goal.progress();

        assertTrue(goal.isAchieved());
    }

    @Test
    void 기록이_없는_상태에서_취소할_수_없다() {
        DailyGoal goal = new DailyGoal(2);

        assertThrows(InvalidProgressException.class, () -> {
            goal.cancel();
        });
    }

    @Test
    void 기록은_취소될_수_있다() {
        DailyGoal goal = new DailyGoal(2);
        goal.progress();

        assertDoesNotThrow(() -> {
            goal.cancel();
        });
    }

    @Test
    void 기록이_취소되어도_성취여부는_변경되지_않는다() {
        DailyGoal goal = new DailyGoal(1);
        goal.progress();

        goal.cancel();
        assertTrue(goal.isAchieved());
    }
}
