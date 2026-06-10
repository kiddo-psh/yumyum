package com.ssafy.manager.growth.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @Embedded
    @AttributeOverride(name = "count", column = @Column(name = "current_streak"))
    private Streak currentStreak;

    @Embedded
    @AttributeOverride(name = "count", column = @Column(name = "max_streak"))
    private Streak maxStreak;

    private LocalDate lastAchievedDate;

    public static MemberStats newFor(Long memberId) {
        MemberStats stats = new MemberStats(Streak.of(0), Streak.of(0), null);
        stats.memberId = memberId;
        return stats;
    }

    public MemberStats(Streak currentStreak, Streak maxStreak, LocalDate lastAchievedDate) {
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.lastAchievedDate = lastAchievedDate;
    }

    public void incrementStreak(LocalDate achievedDate) {
        if (achievedDate.equals(lastAchievedDate)) {
            return;
        }
        currentStreak = isConsecutive(achievedDate) ? currentStreak.increment() : Streak.of(1);
        if (currentStreak.compareTo(maxStreak) > 0) {
            maxStreak = currentStreak;
        }
        lastAchievedDate = achievedDate;
    }

    public void resetStreak() {
        currentStreak = Streak.of(0);
    }

    private boolean isConsecutive(LocalDate achievedDate) {
        return lastAchievedDate != null
                && lastAchievedDate.equals(achievedDate.minusDays(1));
    }
}
