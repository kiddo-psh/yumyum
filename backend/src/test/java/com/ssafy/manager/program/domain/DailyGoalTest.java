package com.ssafy.manager.program.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DailyGoalTest {

    private static final Long ANY_MEMBER = 1L;
    private static final LocalDate ANY_DATE = LocalDate.of(2026, 1, 1);

    private DailyGoal goalWith(double cal, double prot, double carb, double fat) {
        return DailyGoal.of(ANY_MEMBER, ANY_DATE, cal, prot, carb, fat);
    }

    @Test
    void 칼로리_단백질_탄수화물_지방_모두_90에서_110퍼_범위면_달성() {
        DailyGoal goal = goalWith(2000.0, 50.0, 250.0, 70.0);
        goal.recalculate(2000.0, 50.0, 250.0, 70.0);
        assertTrue(goal.isAchieved());
    }

    @Test
    void 단백질이_90퍼_미만이면_미달성() {
        DailyGoal goal = goalWith(2000.0, 50.0, 250.0, 70.0);
        goal.recalculate(2000.0, 44.0, 250.0, 70.0); // 단백질 88%
        assertFalse(goal.isAchieved());
    }

    @Test
    void 탄수화물이_110퍼_초과면_미달성() {
        DailyGoal goal = goalWith(2000.0, 50.0, 250.0, 70.0);
        goal.recalculate(2000.0, 50.0, 278.0, 70.0); // 탄수화물 111.2%
        assertFalse(goal.isAchieved());
    }

    @Test
    void 달성_후_범위_벗어나면_미달성으로_전환된다() {
        DailyGoal goal = goalWith(2000.0, 50.0, 250.0, 70.0);
        goal.recalculate(2000.0, 50.0, 250.0, 70.0);
        assertTrue(goal.isAchieved());

        goal.recalculate(2000.0, 50.0, 278.0, 70.0); // 탄수화물 초과
        assertFalse(goal.isAchieved());
    }

    @Test
    void 경계값_90퍼_정확히_달성() {
        DailyGoal goal = goalWith(2000.0, 50.0, 250.0, 70.0);
        goal.recalculate(1800.0, 45.0, 225.0, 63.0); // 정확히 90%
        assertTrue(goal.isAchieved());
    }

    @Test
    void 경계값_110퍼_정확히_달성() {
        DailyGoal goal = goalWith(2000.0, 50.0, 250.0, 70.0);
        goal.recalculate(2200.0, 55.0, 275.0, 77.0); // 정확히 110%
        assertTrue(goal.isAchieved());
    }
}
