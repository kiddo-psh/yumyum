package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.domain.SplitType;

public record CreateAiRoutineRequest(boolean hasExistingRoutine, int daysPerWeek, SplitType splitType) {}
