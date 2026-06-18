package com.ssafy.manager.global.exception;

import com.ssafy.manager.auth.application.UnauthorizedException;
import com.ssafy.manager.member.domain.OnboardingRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "UNAUTHORIZED", "message", e.getMessage()));
    }

    @ExceptionHandler(OnboardingRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleOnboardingRequired(OnboardingRequiredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code", "ONBOARDING_REQUIRED",
                        "message", e.getMessage(),
                        "resolution", Map.of("method", "PATCH", "href", "/members/me")
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(IllegalStateException e) {
        return ErrorResponse.of(409, "Conflict", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException e) {
        return ErrorResponse.of(403, "Forbidden", e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NoSuchElementException e) {
        return ErrorResponse.of(404, "Not Found", e.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleFastapiDown(RestClientException e) {
        return ErrorResponse.of(503, "Service Unavailable", "AI 서비스에 연결할 수 없습니다.");
    }
}
