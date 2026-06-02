package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.DailySummaryService;
import com.ssafy.manager.nutrition.presentation.dto.DailySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/daily-summary")
@RequiredArgsConstructor
public class DailySummaryController {

    private final DailySummaryService dailySummaryService;

    @GetMapping
    public DailySummaryResponse get(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return dailySummaryService.getSummary(memberId, date);
    }
}
