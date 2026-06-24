package com.ssafy.manager.growth.presentation;

import com.ssafy.manager.growth.application.BadgeCollectionService;
import com.ssafy.manager.growth.presentation.dto.BadgeCollectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeCollectionService badgeCollectionService;

    @GetMapping("/badges")
    public ResponseEntity<BadgeCollectionResponse> collection(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                BadgeCollectionResponse.from(badgeCollectionService.collectionOf(memberId)));
    }
}
