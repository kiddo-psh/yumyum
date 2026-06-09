package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.domain.ProgramType;

import java.time.LocalDate;

public record CreateProgramRequest(
        Long memberId,          // TODO: JWT 도입 후 SecurityContext에서 추출로 교체
        ProgramType healthGoal,
        LocalDate startDate,
        int durationWeeks
) {}
