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
    private int weekNumber;

    private RoutineExercise(Long routineId, String dayLabel, String exerciseName,
                             int targetSets, int targetReps, double targetWeightKg,
                             int orderIndex, int weekNumber) {
        this.routineId = routineId;
        this.dayLabel = dayLabel;
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
        this.orderIndex = orderIndex;
        this.weekNumber = weekNumber;
    }

    public static RoutineExercise create(Long routineId, String dayLabel, String exerciseName,
                                          int targetSets, int targetReps, double targetWeightKg,
                                          int orderIndex) {
        return new RoutineExercise(routineId, dayLabel, exerciseName,
                targetSets, targetReps, targetWeightKg, orderIndex, 1);
    }

    public static RoutineExercise create(Long routineId, String dayLabel, String exerciseName,
                                          int targetSets, int targetReps, double targetWeightKg,
                                          int orderIndex, int weekNumber) {
        return new RoutineExercise(routineId, dayLabel, exerciseName,
                targetSets, targetReps, targetWeightKg, orderIndex, weekNumber);
    }

    public void update(String exerciseName, int targetSets, int targetReps, double targetWeightKg) {
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
    }
}
