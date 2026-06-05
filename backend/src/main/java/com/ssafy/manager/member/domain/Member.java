package com.ssafy.manager.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"oauthProvider", "oauthId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String oauthProvider;
    private String oauthId;
    private String email;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private boolean onboardingCompleted;

    @Enumerated(EnumType.STRING)
    private Sex sex;
    private Integer birthYear;
    private Double heightCm;
    private Double weightKg;
    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    public Member(String oauthProvider, String oauthId, String email) {
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.email = email;
        this.accountStatus = AccountStatus.ACTIVE;
        this.onboardingCompleted = false;
    }

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
