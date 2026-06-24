package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.Badge;
import com.ssafy.manager.growth.domain.MemberBadge;
import com.ssafy.manager.growth.infrastructure.persistence.MemberBadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BadgeCollectionServiceTest {

    @Mock MemberBadgeRepository memberBadgeRepository;

    private static final long MEMBER_ID = 1L;

    private BadgeCollectionService service() {
        return new BadgeCollectionService(memberBadgeRepository);
    }

    @Test
    void 전체_카탈로그를_획득여부와_함께_반환한다() {
        given(memberBadgeRepository.findByMemberId(MEMBER_ID))
                .willReturn(List.of(MemberBadge.newly(MEMBER_ID, Badge.ALL_RIGHT)));

        BadgeCollectionResult result = service().collectionOf(MEMBER_ID);

        assertThat(result.items()).hasSize(Badge.values().length);

        Map<Badge, Boolean> earnedByBadge = result.items().stream()
                .collect(Collectors.toMap(BadgeCollectionResult.Item::badge, BadgeCollectionResult.Item::earned));
        assertThat(earnedByBadge.get(Badge.ALL_RIGHT)).isTrue();
        assertThat(earnedByBadge.get(Badge.WEEKEND_WARRIOR)).isFalse();
    }

    @Test
    void 획득한_뱃지는_earnedAt이_채워지고_미획득은_null이다() {
        given(memberBadgeRepository.findByMemberId(MEMBER_ID))
                .willReturn(List.of(MemberBadge.newly(MEMBER_ID, Badge.NOVICE_TAMER)));

        BadgeCollectionResult result = service().collectionOf(MEMBER_ID);

        BadgeCollectionResult.Item novice = itemOf(result, Badge.NOVICE_TAMER);
        BadgeCollectionResult.Item legendary = itemOf(result, Badge.LEGENDARY_TAMER);
        assertThat(novice.earned()).isTrue();
        assertThat(novice.earnedAt()).isNotNull();
        assertThat(legendary.earned()).isFalse();
        assertThat(legendary.earnedAt()).isNull();
    }

    private BadgeCollectionResult.Item itemOf(BadgeCollectionResult result, Badge badge) {
        return result.items().stream()
                .filter(i -> i.badge() == badge)
                .findFirst()
                .orElseThrow();
    }
}
