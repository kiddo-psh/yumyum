package com.ssafy.manager.nyam.application;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nyam.domain.NyamBodyState;
import com.ssafy.manager.nyam.infrastructure.persistence.NyamBodyStateRepository;
import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NyamBodyStateManagerTest {

    @Mock NyamBodyStateRepository nyamBodyStateRepository;
    @Mock MemberRepository memberRepository;
    @Mock WeightRepository weightRepository;

    @InjectMocks NyamBodyStateManager manager;

    private static final Long MEMBER_ID = 1L;

    @Test
    void 회원_등록_시_anchor_미확정_상태로_생성한다() {
        manager.create(MEMBER_ID);

        ArgumentCaptor<NyamBodyState> captor = ArgumentCaptor.forClass(NyamBodyState.class);
        verify(nyamBodyStateRepository).save(captor.capture());

        NyamBodyState saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.isAnchored()).isFalse();
        assertThat(saved.getAnchorWeightKg()).isZero();
    }

    @Test
    void 온보딩_완료_시_온보딩_체중으로_anchor를_확정한다() {
        Member member = onboardedMember(72.0);
        NyamBodyState state = NyamBodyState.newFor(MEMBER_ID);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(nyamBodyStateRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(state));

        LocalDate onboardedDate = LocalDate.of(2026, 6, 23);
        manager.anchorFromOnboarding(MEMBER_ID, onboardedDate);

        assertThat(state.isAnchored()).isTrue();
        assertThat(state.getAnchorWeightKg()).isEqualTo(72.0);
        assertThat(state.getAnchorDate()).isEqualTo(onboardedDate);
    }

    @Test
    void anchor_미확정_상태면_reAnchor를_건너뛴다() {
        NyamBodyState state = NyamBodyState.newFor(MEMBER_ID);

        manager.reAnchorToLatestWeight(state);

        assertThat(state.isAnchored()).isFalse();
    }

    @Test
    void anchor_이후_실측_체중이_있으면_그_값으로_reAnchor한다() {
        NyamBodyState state = NyamBodyState.newFor(MEMBER_ID, 72.0, LocalDate.of(2026, 6, 23));
        Weight later = Weight.create(MEMBER_ID, 70.0, LocalDate.of(2026, 6, 30));
        given(weightRepository.findTopByMemberIdOrderByRecordedDateDesc(MEMBER_ID))
                .willReturn(Optional.of(later));

        manager.reAnchorToLatestWeight(state);

        assertThat(state.getAnchorWeightKg()).isEqualTo(70.0);
        assertThat(state.getAnchorDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    private Member onboardedMember(double weightKg) {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        member.completeOnboarding(Sex.MALE, 1990, 175.0, weightKg,
                ActivityLevel.MODERATELY_ACTIVE, HealthGoal.DIET);
        return member;
    }
}
