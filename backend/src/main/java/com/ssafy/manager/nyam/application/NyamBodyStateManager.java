package com.ssafy.manager.nyam.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nyam.domain.NyamBodyState;
import com.ssafy.manager.nyam.infrastructure.persistence.NyamBodyStateRepository;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

/**
 * NyamBodyState 생명주기를 관리한다. Member 생명주기에 맞춰 동작한다.
 * - 생성: 회원 등록 시점에 anchor 미확정 상태로 생성한다.
 * - anchor 확정: 온보딩 완료 시점에 온보딩 체중으로 anchor를 확정한다.
 * - 재기준: anchor 이후에 찍힌 실측 체중을 발견하면 그 값으로 re-anchor한다.
 */
@Component
@RequiredArgsConstructor
public class NyamBodyStateManager {

    private final NyamBodyStateRepository nyamBodyStateRepository;
    private final MemberRepository memberRepository;
    private final WeightRepository weightRepository;

    /** 회원 등록 시점: anchor 미확정 NyamBodyState를 생성한다. */
    @Transactional
    public void create(Long memberId) {
        nyamBodyStateRepository.save(NyamBodyState.newFor(memberId));
    }

    /** 온보딩 완료 시점: 온보딩 체중으로 anchor를 확정한다. */
    @Transactional
    public void anchorFromOnboarding(Long memberId, LocalDate onboardedDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        NyamBodyState state = load(memberId);
        state.resetAnchor(member.getWeightKg(), onboardedDate);
    }

    @Transactional(readOnly = true)
    public NyamBodyState load(Long memberId) {
        return nyamBodyStateRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("NyamBodyState를 찾을 수 없습니다."));
    }

    /** anchorDate 이후에 기록된 실측 체중이 있으면 그 값으로 anchor를 리셋한다. */
    @Transactional
    public void reAnchorToLatestWeight(NyamBodyState state) {
        if (!state.isAnchored()) {
            return;
        }
        weightRepository.findTopByMemberIdOrderByRecordedDateDesc(state.getMemberId())
                .filter(weight -> weight.getRecordedDate().isAfter(state.getAnchorDate()))
                .ifPresent(weight -> state.resetAnchor(weight.getWeightKg(), weight.getRecordedDate()));
    }
}
