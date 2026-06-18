package com.ssafy.manager.member.domain;

/**
 * 온보딩이 완료되지 않은 회원이 온보딩 선행이 필요한 작업(예: Program 생성)을
 * 시도할 때 발생한다. 클라이언트를 온보딩 단계로 유도한다.
 */
public class OnboardingRequiredException extends RuntimeException {

    public OnboardingRequiredException(String message) {
        super(message);
    }
}
