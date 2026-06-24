package com.ssafy.manager.growth.application;

import com.ssafy.manager.auth.application.MemberRegisteredEvent;
import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberStatsInitListenerTest {

    @Mock MemberStatsRepository memberStatsRepository;

    @InjectMocks MemberStatsInitListener listener;

    @Test
    void 회원_등록_이벤트를_받으면_해당_회원의_MemberStats를_초기화한다() {
        listener.on(new MemberRegisteredEvent(7L));

        ArgumentCaptor<MemberStats> captor = ArgumentCaptor.forClass(MemberStats.class);
        verify(memberStatsRepository).save(captor.capture());

        MemberStats saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(7L);
        assertThat(saved.getCurrentStreak().count()).isZero();
        assertThat(saved.getMaxStreak().count()).isZero();
        assertThat(saved.getLastAchievedDate()).isNull();
    }
}
