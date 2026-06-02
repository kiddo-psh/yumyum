package com.ssafy.manager.growth.domain;

import jakarta.persistence.Column;
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
    private int currentStreak;
    private int maxStreak;
    private LocalDate lastAchievedDate;

    public static MemberStats newFor(Long memberId) {
        MemberStats stats = new MemberStats(0, 0, null);
        stats.memberId = memberId;
        return stats;
    }

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

    public void resetStreak() {
        currentStreak = 0;
    }

    private boolean isConsecutive(LocalDate achievedDate) {
        return lastAchievedDate != null
                && lastAchievedDate.equals(achievedDate.minusDays(1));
    }
}
