package com.ssafy.manager.auth.application;

/**
 * 신규 회원이 OAuth 가입으로 등록되었음을 알리는 도메인 이벤트.
 * auth는 누가 반응하는지 알지 못하며, 회원 생애주기 시작을 외치기만 한다.
 * 반응 책임(MemberStats 초기화 등)은 각 기능 도메인의 리스너가 가져간다.
 */
public record MemberRegisteredEvent(Long memberId) {
}
