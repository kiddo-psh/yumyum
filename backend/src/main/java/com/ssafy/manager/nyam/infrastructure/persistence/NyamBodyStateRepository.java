package com.ssafy.manager.nyam.infrastructure.persistence;

import com.ssafy.manager.nyam.domain.NyamBodyState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NyamBodyStateRepository extends JpaRepository<NyamBodyState, Long> {

    Optional<NyamBodyState> findByMemberId(Long memberId);
}
