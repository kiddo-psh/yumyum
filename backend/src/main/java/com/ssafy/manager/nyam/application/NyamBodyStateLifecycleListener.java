package com.ssafy.manager.nyam.application;

import com.ssafy.manager.auth.application.MemberRegisteredEvent;
import com.ssafy.manager.member.application.MemberOnboardedEvent;
import com.ssafy.manager.nyam.domain.NyamBodyState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Member 생명주기 이벤트에 반응하여 {@link NyamBodyState} 생명주기를 따라간다.
 * <ul>
 *   <li>회원 등록: anchor 미확정 상태로 NyamBodyState를 생성한다.</li>
 *   <li>온보딩 완료: 온보딩 체중으로 anchor를 확정한다.</li>
 * </ul>
 *
 * <p>동기 {@link EventListener}이므로 발행자의 트랜잭션 안에서 실행된다.
 * 처리 실패 시 발행자(등록/온보딩)도 함께 롤백되어 원자적으로 처리된다.
 */
@Component
@RequiredArgsConstructor
public class NyamBodyStateLifecycleListener {

    private final NyamBodyStateManager stateManager;

    @EventListener
    public void on(MemberRegisteredEvent event) {
        stateManager.create(event.memberId());
    }

    @EventListener
    public void on(MemberOnboardedEvent event) {
        stateManager.anchorFromOnboarding(event.memberId(), LocalDate.now());
    }
}
