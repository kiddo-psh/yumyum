package com.ssafy.manager.auth.presentation;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(@NotBlank String refreshToken) {
}
