package com.ssafy.manager.growth.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Streak implements Comparable<Streak> {

    public static final int MAX_LIMIT = 2_000_000_000;

    private int count;

    public Streak(int count) {
        if (count < 0 || count > MAX_LIMIT) throw new IllegalArgumentException();
        this.count = count;
    }

    public static Streak of(int count) {
        return new Streak(count);
    }

    public Streak increment() {
        return new Streak(count + 1);
    }

    public int count() {
        return count;
    }

    @Override
    public int compareTo(Streak other) {
        return Integer.compare(this.count, other.count);
    }
}
