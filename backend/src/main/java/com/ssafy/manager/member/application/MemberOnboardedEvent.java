package com.ssafy.manager.member.application;

/**
 * 회원이 온보딩(신체 정보 입력)을 완료했음을 알리는 도메인 이벤트.
 * member는 누가 반응하는지 알지 못하며, 온보딩 완료 사실만 외친다.
 * 반응 책임(NyamBodyState anchor 확정 등)은 각 기능 도메인의 리스너가 가져간다.
 *
 * <p>NyamBodyState 자체는 회원 등록({@code MemberRegisteredEvent}) 시점에 생성되지만,
 * anchor 체중은 온보딩에서 입력되는 {@code Member.weightKg}에 의존하므로 이 시점에 확정된다.
 */
public record MemberOnboardedEvent(Long memberId) {
}
