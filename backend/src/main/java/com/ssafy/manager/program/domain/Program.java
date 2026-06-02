package com.ssafy.manager.program.domain;

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

    private Program(Long memberId, ProgramType type, LocalDate startDate,
                    LocalDate endDate, int targetCalories) {
        this.memberId = memberId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.targetCalories = targetCalories;
        this.status = ProgramStatus.ACTIVE;
    }

    public static Program create(Long memberId, ProgramType type, LocalDate startDate,
                                 LocalDate endDate, int targetCalories) {
        return new Program(memberId, type, startDate, endDate, targetCalories);
    }

    public void complete() {
        this.status = ProgramStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == ProgramStatus.ACTIVE;
    }
}
