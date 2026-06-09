package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.Program;

import java.time.LocalDate;

public record ProgramResult(
        Long programId,
        LocalDate startDate,
        LocalDate endDate,
        int durationWeeks,
        int dailyKcal,
        double targetProtein,
        double targetCarb,
        double targetFat,
        String aiComment,
        String status
) {
    public static ProgramResult from(Program program, int durationWeeks) {
        return new ProgramResult(
                program.getId(),
                program.getStartDate(),
                program.getEndDate(),
                durationWeeks,
                program.getTargetCalories(),
                program.getTargetProteinG(),
                program.getTargetCarbG(),
                program.getTargetFatG(),
                program.getAiComment(),
                program.getStatus().name()
        );
    }
}
