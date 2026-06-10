package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.SessionSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionSetRepository extends JpaRepository<SessionSet, Long> {
    List<SessionSet> findBySessionIdIn(List<Long> sessionIds);
}
