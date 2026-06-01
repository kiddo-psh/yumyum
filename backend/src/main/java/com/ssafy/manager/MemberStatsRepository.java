package com.ssafy.manager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberStatsRepository extends JpaRepository<MemberStats, Long> {
    Optional<MemberStats> findByMemberId(Long memberId);
}
