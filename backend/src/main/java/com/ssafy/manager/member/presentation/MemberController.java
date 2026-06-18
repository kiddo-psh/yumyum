package com.ssafy.manager.member.presentation;

import com.ssafy.manager.member.application.MemberService;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.presentation.dto.MemberResponse;
import com.ssafy.manager.member.presentation.dto.OnboardingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> onboard(
            @AuthenticationPrincipal Long memberId,
            @RequestBody OnboardingRequest request
    ) {
        Member member = memberService.completeOnboarding(memberId, request.toCommand());
        return ResponseEntity.ok(MemberResponse.from(member));
    }
}
