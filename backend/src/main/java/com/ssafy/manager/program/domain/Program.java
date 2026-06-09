package com.ssafy.manager.program.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    @Enumerated(EnumType.STRING)
    private ProgramType type;
    private LocalDate startDate;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private ProgramStatus status;
    private int targetCalories;
    private double targetProteinG;
    private double targetCarbG;
    private double targetFatG;
    @Column(length = 1000)
    private String aiComment;

    private Program(Long memberId, ProgramType type, LocalDate startDate, LocalDate endDate,
                    int targetCalories, double targetProteinG, double targetCarbG,
                    double targetFatG, String aiComment) {
        this.memberId = memberId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.targetCalories = targetCalories;
        this.targetProteinG = targetProteinG;
        this.targetCarbG = targetCarbG;
        this.targetFatG = targetFatG;
        this.aiComment = aiComment;
        this.status = ProgramStatus.ACTIVE;
    }

    public static Program create(Long memberId, ProgramType type, LocalDate startDate,
                                 LocalDate endDate, int targetCalories,
                                 double targetProteinG, double targetCarbG,
                                 double targetFatG, String aiComment) {
        return new Program(memberId, type, startDate, endDate, targetCalories,
                targetProteinG, targetCarbG, targetFatG, aiComment);
    }

    public void complete() {
        this.status = ProgramStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == ProgramStatus.ACTIVE;
    }
}
