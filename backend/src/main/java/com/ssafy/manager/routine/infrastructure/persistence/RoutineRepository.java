package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findByMemberId(Long memberId);

    Optional<Routine> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
}
