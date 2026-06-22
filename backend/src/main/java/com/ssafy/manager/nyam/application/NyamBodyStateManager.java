package com.ssafy.manager.nyam.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nyam.domain.NyamBodyState;
import com.ssafy.manager.nyam.infrastructure.persistence.NyamBodyStateRepository;
import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

/**
 * NyamBodyState 생명주기를 상위 도메인 호출 없이 스스로 관리한다(pull).
 * - 생성: 상태가 없으면 최신 실측 체중(없으면 온보딩 체중)을 anchor로 lazy 생성한다.
 * - 재기준: anchor 이후에 찍힌 실측 체중을 발견하면 그 값으로 re-anchor한다.
 */
@Component
@RequiredArgsConstructor
public class NyamBodyStateManager {

    private final NyamBodyStateRepository nyamBodyStateRepository;
    private final MemberRepository memberRepository;
    private final WeightRepository weightRepository;

    @Transactional
    public NyamBodyState loadOrCreate(Long memberId, LocalDate asOfDate) {
        return nyamBodyStateRepository.findByMemberId(memberId)
                .orElseGet(() -> nyamBodyStateRepository.save(create(memberId, asOfDate)));
    }

    /** anchorDate 이후에 기록된 실측 체중이 있으면 그 값으로 anchor를 리셋한다. */
    @Transactional
    public void reAnchorToLatestWeight(NyamBodyState state) {
        weightRepository.findTopByMemberIdOrderByRecordedDateDesc(state.getMemberId())
                .filter(weight -> weight.getRecordedDate().isAfter(state.getAnchorDate()))
                .ifPresent(weight -> state.resetAnchor(weight.getWeightKg(), weight.getRecordedDate()));
    }

    private NyamBodyState create(Long memberId, LocalDate asOfDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        Weight latest = weightRepository.findTopByMemberIdOrderByRecordedDateDesc(memberId).orElse(null);

        double anchorWeightKg = latest != null ? latest.getWeightKg() : member.getWeightKg();
        LocalDate anchorDate = latest != null ? latest.getRecordedDate() : asOfDate;
        return NyamBodyState.newFor(memberId, anchorWeightKg, anchorDate);
    }
}
