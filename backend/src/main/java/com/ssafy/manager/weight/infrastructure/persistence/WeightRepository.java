package com.ssafy.manager.weight.infrastructure.persistence;

import com.ssafy.manager.weight.domain.Weight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightRepository extends JpaRepository<Weight, Long> {
    List<Weight> findByMemberIdOrderByRecordedDateDesc(Long memberId);

    Optional<Weight> findTopByMemberIdOrderByRecordedDateDesc(Long memberId);

    List<Weight> findByMemberIdAndRecordedDateBetween(Long memberId, LocalDate from, LocalDate to);
}
