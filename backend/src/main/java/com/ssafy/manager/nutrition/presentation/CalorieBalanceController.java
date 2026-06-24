package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.CalorieBalanceService;
import com.ssafy.manager.nutrition.presentation.dto.CalorieBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/calorie-balance")
@RequiredArgsConstructor
public class CalorieBalanceController {

    private final CalorieBalanceService calorieBalanceService;

    @GetMapping
    public ResponseEntity<CalorieBalanceResponse> getBalance(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(calorieBalanceService.getBalance(memberId, targetDate, LocalTime.now()));
    }
}
