package com.ssafy.manager.weight.presentation;

import java.time.LocalDate;

public record CreateWeightRequest(Long memberId, double weightKg, LocalDate recordedDate) {}
