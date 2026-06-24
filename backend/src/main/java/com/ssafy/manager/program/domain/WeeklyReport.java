package com.ssafy.manager.program.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long programId;
    private int weekNumber;

    @Column(length = 2000)
    private String content;

    @Column(length = 1000)
    private String nutritionSummary;

    @Column(length = 1000)
    private String exerciseSummary;

    @Column(length = 1000)
    private String goalSummary;

    private double avgCalorieRate;
    private int achievementDays;
    private Double weightTrend;

    public WeeklyReport(Long programId, int weekNumber) {
        this.programId = programId;
        this.weekNumber = weekNumber;
    }

    public void fill(String content, String nutritionSummary, String exerciseSummary,
                     String goalSummary, double avgCalorieRate, int achievementDays,
                     Double weightTrend) {
        this.content = content;
        this.nutritionSummary = nutritionSummary;
        this.exerciseSummary = exerciseSummary;
        this.goalSummary = goalSummary;
        this.avgCalorieRate = avgCalorieRate;
        this.achievementDays = achievementDays;
        this.weightTrend = weightTrend;
    }
}
