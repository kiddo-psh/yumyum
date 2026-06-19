package com.ssafy.manager.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        @JsonInclude(Include.NON_NULL) Map<String, String> errors
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), null);
    }

    public static ErrorResponse of (int status, String error, String message, Map<String, String> errors) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), errors);
    }
}
