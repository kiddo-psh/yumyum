package com.ssafy.manager;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MemberStats {

    private int currentStreak;
    private int maxStreak;
    private LocalDate lastAchievedDate;

    public MemberStats(int currentStreak, int maxStreak, LocalDate lastAchievedDate) {
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.lastAchievedDate = lastAchievedDate;
    }

    public void incrementStreak(LocalDate achievedDate) {
        currentStreak = isConsecutive(achievedDate) ? currentStreak + 1 : 1;
        if (currentStreak > maxStreak) {
            maxStreak = currentStreak;
        }
        lastAchievedDate = achievedDate;
    }

    private boolean isConsecutive(LocalDate achievedDate) {
        return lastAchievedDate != null
                && lastAchievedDate.equals(achievedDate.minusDays(1));
    }
}
