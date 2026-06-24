package com.ssafy.manager.growth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 특정 Member가 특정 {@link Badge}를 획득한 기록 (docs/CONTEXT.md).
 *
 * <p>(member_id, badge) 쌍은 유일하다 — 같은 뱃지를 두 번 획득할 수 없다.
 * 멱등 보장의 최종 방어선이 이 unique 제약이다 (docs/adr/0002).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_member_badge", columnNames = {"member_id", "badge"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Badge badge;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    private MemberBadge(Long memberId, Badge badge) {
        this.memberId = memberId;
        this.badge = badge;
        this.earnedAt = LocalDateTime.now();
    }

    public static MemberBadge newly(Long memberId, Badge badge) {
        return new MemberBadge(memberId, badge);
    }
}
