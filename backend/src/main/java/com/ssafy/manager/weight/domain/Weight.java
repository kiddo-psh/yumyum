package com.ssafy.manager.weight.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private double weightKg;
    private LocalDate recordedDate;

    private Weight(Long memberId, double weightKg, LocalDate recordedDate) {
        this.memberId = memberId;
        this.weightKg = weightKg;
        this.recordedDate = recordedDate;
    }

    public static Weight create(Long memberId, double weightKg, LocalDate recordedDate) {
        return new Weight(memberId, weightKg, recordedDate);
    }
}
