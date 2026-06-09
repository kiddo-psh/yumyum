package com.ssafy.manager.growth.presentation;

import com.ssafy.manager.growth.application.DailyGoalSummaryService;
import com.ssafy.manager.growth.domain.DailyProgress;
import com.ssafy.manager.growth.domain.WeeklyAchievementSummary;
import com.ssafy.manager.growth.presentation.dto.WeeklyCalendarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final DailyGoalSummaryService dailyGoalSummaryService;

    @GetMapping("/weekly-calendar")
    public ResponseEntity<WeeklyCalendarResponse> weeklyCalendar(
            @AuthenticationPrincipal Long memberId) {
        WeeklyAchievementSummary summary = dailyGoalSummaryService.weeklyCalendar(memberId, LocalDate.now());
        return ResponseEntity.ok(WeeklyCalendarResponse.from(summary));
    }

    @GetMapping("/progress")
    public ResponseEntity<DailyProgress> progress(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(dailyGoalSummaryService.todayProgress(memberId, LocalDate.now()));
    }
}
