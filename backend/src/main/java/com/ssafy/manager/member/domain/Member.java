package com.ssafy.manager.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Sex sex;
    private int birthYear;
    private double heightCm;
    private double weightKg;
    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    public Member(Sex sex, int birthYear, double heightCm, double weightKg, ActivityLevel activityLevel) {
        this.sex = sex;
        this.birthYear = birthYear;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityLevel = activityLevel;
    }

    public int age(int currentYear) {
        return currentYear - birthYear;
    }
}
