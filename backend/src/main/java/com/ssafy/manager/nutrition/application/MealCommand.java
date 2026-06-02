package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.MealType;

import java.time.LocalDate;
import java.util.List;

public record MealCommand(Long memberId, MealType type, LocalDate date, List<MealItemCommand> items) {
}
