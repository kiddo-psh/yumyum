package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.application.ProgramResult;

import java.time.LocalDate;

public record CreateProgramResponse(
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
    public static CreateProgramResponse from(ProgramResult result) {
        return new CreateProgramResponse(
                result.programId(),
                result.startDate(),
                result.endDate(),
                result.durationWeeks(),
                result.dailyKcal(),
                result.targetProtein(),
                result.targetCarb(),
                result.targetFat(),
                result.aiComment(),
                result.status()
        );
    }
}
