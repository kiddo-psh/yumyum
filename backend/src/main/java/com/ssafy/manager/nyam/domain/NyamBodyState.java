package com.ssafy.manager.nyam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Nyam 체형 상태. 실측 체중(anchor) + 그 이후 누적 칼로리 잉여/결손으로
 * 가상 체중을 추정하고, 키와 결합해 BMI 체형 카테고리를 산출한다.
 *
 * stateful: 매일 배치로 전날 밸런스를 누적 반영하고, 체중 기록 시 anchor를 리셋한다.
 * BODYBUILDER 여부는 여기 저장하지 않고 조회 시 운동 실적으로 즉석 판정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NyamBodyState {

    /** 체지방 1kg ≈ 7700kcal (경험칙). */
    private static final double KCAL_PER_KG = 7700.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    private double anchorWeightKg;
    private LocalDate anchorDate;
    private double cumulativeBalanceKcal;

    private NyamBodyState(Long memberId, double anchorWeightKg, LocalDate anchorDate) {
        this.memberId = memberId;
        this.anchorWeightKg = anchorWeightKg;
        this.anchorDate = anchorDate;
        this.cumulativeBalanceKcal = 0;
    }

    public static NyamBodyState newFor(Long memberId, double anchorWeightKg, LocalDate anchorDate) {
        return new NyamBodyState(memberId, anchorWeightKg, anchorDate);
    }

    /** 하루치 칼로리 밸런스(잉여 +, 결손 −)를 누적한다. */
    public void applyDailyBalance(double balanceKcal) {
        this.cumulativeBalanceKcal += balanceKcal;
    }

    /** 실측 체중 기록 시 anchor를 그 값으로 리셋하고 누적 추정치를 비운다. */
    public void resetAnchor(double weightKg, LocalDate date) {
        this.anchorWeightKg = weightKg;
        this.anchorDate = date;
        this.cumulativeBalanceKcal = 0;
    }

    public double virtualWeightKg() {
        return anchorWeightKg + cumulativeBalanceKcal / KCAL_PER_KG;
    }
}
