package com.ssafy.manager.program.domain;

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
public class DailyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate date;

    private double targetValue;
    private double achievedValue = 0;
    private boolean achieved = false;

    public DailyGoal(double targetValue) {
        this.targetValue = targetValue;
    }

    public static DailyGoal of(Long memberId, LocalDate date, double targetValue) {
        DailyGoal goal = new DailyGoal(targetValue);
        goal.memberId = memberId;
        goal.date = date;
        return goal;
    }

    public void recalculate(double value) {
        achievedValue = value;
        if (!achieved) {
            achieved = targetValue <= achievedValue;
        }
    }
}
