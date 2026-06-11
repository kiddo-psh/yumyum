package com.ssafy.manager.growth.infrastructure.persistence;

import com.ssafy.manager.growth.domain.MemberStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberStatsRepository extends JpaRepository<MemberStats, Long> {
    Optional<MemberStats> findByMemberId(Long memberId);

    @Query("SELECT ms FROM MemberStats ms WHERE ms.lastAchievedDate IS NULL OR ms.lastAchievedDate < :yesterday")
    List<MemberStats> findAllWithExpiredStreak(@Param("yesterday") LocalDate yesterday);
}
