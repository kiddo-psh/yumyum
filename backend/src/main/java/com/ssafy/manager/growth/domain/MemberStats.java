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

    /**
     * 주어진 날짜로 스트릭을 증가시킨다.
     *
     * @return 실제로 증가했으면 {@code true}, 같은 날 재달성 등으로 변화가 없으면 {@code false}.
     *         이 반환값으로 {@code StreakIncreasedEvent} 중복 발행을 막는다.
     */
    public boolean incrementStreak(LocalDate achievedDate) {
        if (lastAchievedDate != null && !achievedDate.isAfter(lastAchievedDate)) {
            return false;
        }
        currentStreak = isConsecutive(achievedDate) ? currentStreak.increment() : Streak.of(1);
        if (currentStreak.compareTo(maxStreak) > 0) {
            maxStreak = currentStreak;
        }
        lastAchievedDate = achievedDate;
        return true;
    }

    public boolean isStreakExpired(LocalDate today) {
        return lastAchievedDate == null || !lastAchievedDate.equals(today.minusDays(1));
    }

    public void resetStreak() {
        currentStreak = Streak.of(0);
    }

    private boolean isConsecutive(LocalDate achievedDate) {
        return lastAchievedDate != null
                && lastAchievedDate.equals(achievedDate.minusDays(1));
    }
}
