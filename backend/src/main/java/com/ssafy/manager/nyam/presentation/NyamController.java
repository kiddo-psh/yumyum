package com.ssafy.manager.nyam.presentation;

import com.ssafy.manager.nyam.application.NyamBodyQueryService;
import com.ssafy.manager.nyam.application.NyamBodyResult;
import com.ssafy.manager.nyam.presentation.dto.NyamBodyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/nyam")
@RequiredArgsConstructor
public class NyamController {

    private final NyamBodyQueryService nyamBodyQueryService;

    @GetMapping("/body")
    public ResponseEntity<NyamBodyResponse> getBody(@AuthenticationPrincipal Long memberId) {
        NyamBodyResult result = nyamBodyQueryService.getBody(memberId, LocalDate.now());
        return ResponseEntity.ok(NyamBodyResponse.from(result));
    }
}
