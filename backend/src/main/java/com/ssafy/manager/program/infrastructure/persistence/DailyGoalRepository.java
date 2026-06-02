package com.ssafy.manager.program.infrastructure.persistence;

import com.ssafy.manager.program.domain.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {

    Optional<DailyGoal> findByMemberIdAndDate(Long memberId, LocalDate date);

    List<DailyGoal> findAllByDate(LocalDate date);

    boolean existsByMemberIdAndDate(Long memberId, LocalDate date);
}
