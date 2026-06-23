package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {

    interface DailyCaloriesSummary {
        java.time.LocalDate getSessionDate();
        long getTotalCalories();
    }

    List<RoutineSession> findTop4ByRoutineIdOrderBySessionDateDesc(Long routineId);

    @Query("SELECT COALESCE(SUM(rs.caloriesBurned), 0) FROM RoutineSession rs WHERE rs.memberId = :memberId AND rs.sessionDate = :date")
    long sumCaloriesBurnedByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);

    @Query("SELECT rs.sessionDate AS sessionDate, COALESCE(SUM(rs.caloriesBurned), 0) AS totalCalories " +
           "FROM RoutineSession rs WHERE rs.memberId = :memberId AND rs.sessionDate BETWEEN :from AND :to " +
           "GROUP BY rs.sessionDate")
    List<DailyCaloriesSummary> findDailyCaloriesBurnedByMemberIdAndDateBetween(
            @Param("memberId") Long memberId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COUNT(DISTINCT rs.sessionDate) FROM RoutineSession rs WHERE rs.memberId = :memberId AND rs.sessionDate BETWEEN :from AND :to")
    int countDistinctSessionDatesByMemberIdAndDateBetween(@Param("memberId") Long memberId,
                                                          @Param("from") LocalDate from,
                                                          @Param("to") LocalDate to);

    List<RoutineSession> findByMemberIdAndSessionDateBetween(Long memberId, LocalDate from, LocalDate to);
}
