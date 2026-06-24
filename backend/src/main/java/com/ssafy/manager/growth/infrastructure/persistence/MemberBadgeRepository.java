package com.ssafy.manager.growth.infrastructure.persistence;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.growth.domain.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {

    boolean existsByMemberIdAndBadge(Long memberId, Badge badge);

    List<MemberBadge> findByMemberId(Long memberId);
}
