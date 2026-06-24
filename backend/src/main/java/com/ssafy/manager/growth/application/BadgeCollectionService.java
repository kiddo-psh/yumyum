package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.growth.domain.MemberBadge;
import com.ssafy.manager.growth.infrastructure.persistence.MemberBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 뱃지 컬렉션(G602) 조회. 전체 {@link Badge} 카탈로그를 회원의 획득 기록과 left-join 한다.
 *
 * <p>잠긴 뱃지도 그대로 노출하는 조건 공개 정책을 따른다 — 카탈로그 전체를 항상 반환한다.
 */
@Service
@RequiredArgsConstructor
public class BadgeCollectionService {

    private final MemberBadgeRepository memberBadgeRepository;

    @Transactional(readOnly = true)
    public BadgeCollectionResult collectionOf(Long memberId) {
        Map<Badge, LocalDateTime> earnedAtByBadge = new EnumMap<>(Badge.class);
        for (MemberBadge memberBadge : memberBadgeRepository.findByMemberId(memberId)) {
            earnedAtByBadge.put(memberBadge.getBadge(), memberBadge.getEarnedAt());
        }

        List<BadgeCollectionResult.Item> items = Arrays.stream(Badge.values())
                .map(badge -> new BadgeCollectionResult.Item(
                        badge,
                        earnedAtByBadge.containsKey(badge),
                        earnedAtByBadge.get(badge)))
                .toList();

        return new BadgeCollectionResult(items);
    }
}
