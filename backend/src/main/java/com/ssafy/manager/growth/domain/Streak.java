package com.ssafy.manager.growth.domain;

import java.time.LocalDate;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Streak {

    public static final int MAX_LIMIT = 2_000_000_000;

    private int count;
    private LocalDate lastAchievedDate;

    public Streak(int count, LocalDate lastAchievedDate) {
        if (count < 0 || count > MAX_LIMIT) throw new IllegalArgumentException();

        this.count = count;
        this.lastAchievedDate = lastAchievedDate;
    }

    public static Streak of(int count, LocalDate date) {
        return new Streak(count, date);
    }

    public Streak increment(LocalDate date) {
        if (lastAchievedDate.equals(date)) {
            return this;
        }

        int nextCount = isConsecutive(date) ? count + 1 : 1;
        return new Streak(nextCount, date);
    }

    public Streak reset() {
        return new Streak(0, lastAchievedDate);
    }

    private boolean isConsecutive(LocalDate date) {
        return lastAchievedDate != null && lastAchievedDate.plusDays(1).equals(date);
    }
}
