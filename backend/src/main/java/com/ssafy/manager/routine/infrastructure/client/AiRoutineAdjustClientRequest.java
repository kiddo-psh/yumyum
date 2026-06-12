package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineAdjustClientRequest(
        @JsonProperty("routine_id") Long routineId,
        @JsonProperty("current_week_number") int currentWeekNumber,
        @JsonProperty("exercises") List<ExerciseInfo> exercises,
        @JsonProperty("recent_sessions") List<RecentSession> recentSessions
) {
    public record ExerciseInfo(
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("day_label") String dayLabel,
            @JsonProperty("exercise_name") String exerciseName,
            @JsonProperty("target_sets") int targetSets,
            @JsonProperty("target_reps") int targetReps,
            @JsonProperty("target_weight_kg") double targetWeightKg,
            @JsonProperty("order_index") int orderIndex
    ) {}

    public record RecentSession(
            @JsonProperty("session_date") String sessionDate,
            @JsonProperty("sets") List<SessionSetData> sets
    ) {}

    public record SessionSetData(
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("exercise_name") String exerciseName,
            @JsonProperty("target_sets") int targetSets,
            @JsonProperty("actual_sets_completed") int actualSetsCompleted,
            @JsonProperty("avg_actual_reps") double avgActualReps,
            @JsonProperty("avg_actual_weight_kg") double avgActualWeightKg
    ) {}
}
