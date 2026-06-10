package com.ssafy.manager.weight.presentation;

import java.time.LocalDate;

public record CreateWeightRequest(double weightKg, LocalDate recordedDate) {}
