package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.growth.domain.MemberBadge;
import com.ssafy.manager.growth.infrastructure.persistence.MemberBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 뱃지 발행(grant). 멱등이다 — 이미 보유한 뱃지는 다시 발행하지 않는다 (docs/adr/0002).
 *
 * <p>2중 방어: 앱 레벨 {@code existsByMemberIdAndBadge} 확인 + DB의 (member_id, badge)
 * unique 제약. 새로 발행될 때만 {@link EarnedBadgeCollector}에 적재해 piggyback 응답에 실린다.
 */
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final MemberBadgeRepository memberBadgeRepository;
    private final EarnedBadgeCollector earnedBadgeCollector;

    public void grant(Long memberId, Badge badge) {
        if (memberBadgeRepository.existsByMemberIdAndBadge(memberId, badge)) {
            return;
        }
        memberBadgeRepository.save(MemberBadge.newly(memberId, badge));
        earnedBadgeCollector.add(badge);
    }
}
