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
public class RoutineExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routineId;
    private String dayLabel;
    private String exerciseName;
    private int targetSets;
    private int targetReps;
    private double targetWeightKg;
    private int orderIndex;

    private RoutineExercise(Long routineId, String dayLabel, String exerciseName,
                             int targetSets, int targetReps, double targetWeightKg, int orderIndex) {
        this.routineId = routineId;
        this.dayLabel = dayLabel;
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
        this.orderIndex = orderIndex;
    }

    public static RoutineExercise create(Long routineId, String dayLabel, String exerciseName,
                                          int targetSets, int targetReps, double targetWeightKg,
                                          int orderIndex) {
        return new RoutineExercise(routineId, dayLabel, exerciseName,
                targetSets, targetReps, targetWeightKg, orderIndex);
    }

    public void update(String exerciseName, int targetSets, int targetReps, double targetWeightKg) {
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
    }
}
