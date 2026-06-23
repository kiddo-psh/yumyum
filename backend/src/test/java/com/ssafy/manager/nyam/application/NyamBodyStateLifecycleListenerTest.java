package com.ssafy.manager.nyam.application;

import com.ssafy.manager.auth.application.MemberRegisteredEvent;
import com.ssafy.manager.member.application.MemberOnboardedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NyamBodyStateLifecycleListenerTest {

    @Mock NyamBodyStateManager stateManager;

    @InjectMocks NyamBodyStateLifecycleListener listener;

    @Test
    void 회원_등록_이벤트를_받으면_anchor_미확정_NyamBodyState를_생성한다() {
        listener.on(new MemberRegisteredEvent(7L));

        verify(stateManager).create(7L);
    }

    @Test
    void 온보딩_완료_이벤트를_받으면_온보딩_체중으로_anchor를_확정한다() {
        listener.on(new MemberOnboardedEvent(7L));

        verify(stateManager).anchorFromOnboarding(eq(7L), any(LocalDate.class));
    }
}
