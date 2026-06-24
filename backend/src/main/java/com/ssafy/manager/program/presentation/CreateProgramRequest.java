package com.ssafy.manager.program.presentation;

import java.time.LocalDate;

public record CreateProgramRequest(
        Long memberId,          // TODO: JWT 도입 후 SecurityContext에서 추출로 교체
        LocalDate startDate,
        int durationWeeks
) {}
