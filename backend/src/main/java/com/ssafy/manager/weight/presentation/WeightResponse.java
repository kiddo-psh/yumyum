package com.ssafy.manager.weight.presentation;

import com.ssafy.manager.weight.domain.Weight;

import java.time.LocalDate;

public record WeightResponse(Long id, double weightKg, LocalDate recordedDate) {
    public static WeightResponse from(Weight w) {
        return new WeightResponse(w.getId(), w.getWeightKg(), w.getRecordedDate());
    }
}
