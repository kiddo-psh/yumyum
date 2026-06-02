package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.DailySummaryService;
import com.ssafy.manager.nutrition.presentation.dto.DailySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/daily-summary")
@RequiredArgsConstructor
public class DailySummaryController {

    private final DailySummaryService dailySummaryService;

    @GetMapping
    public ResponseEntity<DailySummaryResponse> get(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(dailySummaryService.getSummary(memberId, targetDate));
    }
}
