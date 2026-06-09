package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.domain.SplitType;

public record CreateAiRoutineRequest(Long memberId, int daysPerWeek, SplitType splitType) {}
