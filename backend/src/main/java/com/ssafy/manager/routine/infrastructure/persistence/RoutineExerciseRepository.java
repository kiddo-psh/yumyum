package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoutineExerciseRepository extends JpaRepository<RoutineExercise, Long> {

    List<RoutineExercise> findByRoutineIdOrderByDayLabelAscOrderIndexAsc(Long routineId);

    List<RoutineExercise> findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(
            Long routineId, int weekNumber);

    @Query("SELECT COALESCE(MAX(re.weekNumber), 1) FROM RoutineExercise re WHERE re.routineId = :routineId")
    int findMaxWeekNumberByRoutineId(@Param("routineId") Long routineId);
}
