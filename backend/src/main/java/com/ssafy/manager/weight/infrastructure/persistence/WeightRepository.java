package com.ssafy.manager.weight.infrastructure.persistence;

import com.ssafy.manager.weight.domain.Weight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeightRepository extends JpaRepository<Weight, Long> {
    List<Weight> findByMemberIdOrderByRecordedDateDesc(Long memberId);
}
