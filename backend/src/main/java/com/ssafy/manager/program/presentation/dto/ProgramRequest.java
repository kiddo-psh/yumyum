package com.ssafy.manager.program.presentation.dto;

import com.ssafy.manager.program.domain.ProgramType;

import java.time.LocalDate;

public record ProgramRequest(ProgramType type, LocalDate startDate, LocalDate endDate) {}
