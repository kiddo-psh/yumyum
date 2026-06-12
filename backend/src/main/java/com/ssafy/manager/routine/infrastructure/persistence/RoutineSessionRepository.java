package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {
    List<RoutineSession> findTop4ByRoutineIdOrderBySessionDateDesc(Long routineId);

    @Query("SELECT COALESCE(SUM(rs.caloriesBurned), 0) FROM RoutineSession rs WHERE rs.memberId = :memberId AND rs.sessionDate = :date")
    long sumCaloriesBurnedByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
}
