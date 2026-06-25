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

    private double targetProteinG;
    private double achievedProteinG;
    private double targetCarbG;
    private double achievedCarbG;
    private double targetFatG;
    private double achievedFatG;

    private boolean achieved = false;

    private DailyGoal(double targetValue, double targetProteinG, double targetCarbG, double targetFatG) {
        this.targetValue = targetValue;
        this.targetProteinG = targetProteinG;
        this.targetCarbG = targetCarbG;
        this.targetFatG = targetFatG;
    }

    public static DailyGoal of(Long memberId, LocalDate date, double targetValue) {
        return of(memberId, date, targetValue, 0, 0, 0);
    }

    public static DailyGoal of(Long memberId, LocalDate date,
                                double targetValue, double targetProteinG,
                                double targetCarbG, double targetFatG) {
        DailyGoal goal = new DailyGoal(targetValue, targetProteinG, targetCarbG, targetFatG);
        goal.memberId = memberId;
        goal.date = date;
        return goal;
    }

    public void recalculate(double calories, double proteinG, double carbG, double fatG) {
        achievedValue = calories;
        achievedProteinG = proteinG;
        achievedCarbG = carbG;
        achievedFatG = fatG;
        achieved = isWithin(targetValue, calories);
    }

    private boolean isWithin(double target, double achieved) {
        if (target == 0) return true;
        return target * 0.9 <= achieved && achieved <= target * 1.1;
    }
}
