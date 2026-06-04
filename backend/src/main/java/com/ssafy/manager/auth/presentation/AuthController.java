package com.ssafy.manager.auth.presentation;

import com.ssafy.manager.auth.application.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        authService.logout(memberId);
        return ResponseEntity.noContent().build();
    }
}
