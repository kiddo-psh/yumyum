package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private String name;
    private int daysPerWeek;
    private boolean aiGenerated;
    private LocalDateTime createdAt;

    private Routine(Long memberId, String name, int daysPerWeek, boolean aiGenerated) {
        this.memberId = memberId;
        this.name = name;
        this.daysPerWeek = daysPerWeek;
        this.aiGenerated = aiGenerated;
        this.createdAt = LocalDateTime.now();
    }

    public static Routine create(Long memberId, String name, int daysPerWeek, boolean aiGenerated) {
        return new Routine(memberId, name, daysPerWeek, aiGenerated);
    }
}
