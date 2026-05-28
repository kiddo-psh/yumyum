package com.ssafy.manager;

public class DailyGoal {
    private final int targetValue;
    private int achievedCount = 0;
    private boolean achieved = false;

    public DailyGoal(int targetValue) {
        this.targetValue = targetValue;
    }

    public void progress() {
        achievedCount += 1;
        if (achievedCount >= targetValue) {
            achieved = true;
        }
    }

    public void cancel() {
        if (achievedCount == 0) {
            throw new InvalidProgressException("달성 횟수를 0보다 아래로 내릴 수 없습니다.");
        }
        achievedCount -= 1;
    }

    public boolean isAchieved() {
        return achieved;
    }
}
