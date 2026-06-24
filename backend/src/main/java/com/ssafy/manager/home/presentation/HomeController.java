package com.ssafy.manager.home.presentation;

import com.ssafy.manager.home.application.HomeCommentService;
import com.ssafy.manager.home.presentation.dto.HomeCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeCommentService homeCommentService;

    @GetMapping("/home/comment")
    public ResponseEntity<HomeCommentResponse> getComment(
            @AuthenticationPrincipal Long memberId) {
        String comment = homeCommentService.getComment(memberId);
        return ResponseEntity.ok(new HomeCommentResponse(comment));
    }
}
