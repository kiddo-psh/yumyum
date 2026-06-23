package com.ssafy.manager.growth.application;

import com.ssafy.manager.auth.application.MemberRegisteredEvent;
import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 신규 회원 등록에 반응하여 스트릭 집계용 {@link MemberStats}를 초기화한다.
 *
 * <p>동기 {@link EventListener}이므로 발행자(MemberOAuthService)의 트랜잭션 안에서 실행된다.
 * 초기화 실패 시 회원 등록도 함께 롤백되어, 회원과 MemberStats는 원자적으로 생성된다.
 */
@Component
@RequiredArgsConstructor
public class MemberStatsInitListener {

    private final MemberStatsRepository memberStatsRepository;

    @EventListener
    public void on(MemberRegisteredEvent event) {
        memberStatsRepository.save(MemberStats.newFor(event.memberId()));
    }
}
