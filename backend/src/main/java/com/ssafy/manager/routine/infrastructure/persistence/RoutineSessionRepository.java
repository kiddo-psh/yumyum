package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {
    List<RoutineSession> findTop4ByRoutineIdOrderBySessionDateDesc(Long routineId);
}
