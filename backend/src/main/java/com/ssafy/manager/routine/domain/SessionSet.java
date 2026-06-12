package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sessionId;
    private Long exerciseId;
    private String exerciseName;
    private int setNumber;
    private int actualReps;
    private double actualWeightKg;
    private boolean completed;

    private SessionSet(Long sessionId, Long exerciseId, String exerciseName,
                       int setNumber, int actualReps, double actualWeightKg, boolean completed) {
        this.sessionId = sessionId;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.setNumber = setNumber;
        this.actualReps = actualReps;
        this.actualWeightKg = actualWeightKg;
        this.completed = completed;
    }

    public static SessionSet create(Long sessionId, Long exerciseId, String exerciseName,
                                     int setNumber, int actualReps, double actualWeightKg,
                                     boolean completed) {
        return new SessionSet(sessionId, exerciseId, exerciseName,
                setNumber, actualReps, actualWeightKg, completed);
    }
}
