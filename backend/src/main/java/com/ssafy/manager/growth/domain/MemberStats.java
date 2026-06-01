package com.ssafy.manager.growth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class MemberStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
