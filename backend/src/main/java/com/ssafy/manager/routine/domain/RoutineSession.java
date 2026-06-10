package com.ssafy.manager.routine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routineId;
    private Long memberId;
    private LocalDate sessionDate;
    private LocalDateTime completedAt;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int caloriesBurned;

    private RoutineSession(Long routineId, Long memberId, LocalDate sessionDate, int caloriesBurned) {
        this.routineId = routineId;
        this.memberId = memberId;
        this.sessionDate = sessionDate;
        this.completedAt = LocalDateTime.now();
        this.caloriesBurned = caloriesBurned;
    }

    public static RoutineSession create(Long routineId, Long memberId, LocalDate sessionDate) {
        return new RoutineSession(routineId, memberId, sessionDate, 0);
    }

    public static RoutineSession create(Long routineId, Long memberId, LocalDate sessionDate, int caloriesBurned) {
        return new RoutineSession(routineId, memberId, sessionDate, caloriesBurned);
    }
}
