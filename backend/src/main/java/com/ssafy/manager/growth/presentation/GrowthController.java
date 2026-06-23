package com.ssafy.manager.growth.presentation;

import com.ssafy.manager.growth.application.DailyGoalSummaryService;
import com.ssafy.manager.growth.domain.DailyProgress;
import com.ssafy.manager.growth.domain.WeeklyAchievementSummary;
import com.ssafy.manager.growth.presentation.dto.NyamStatusResponse;
import com.ssafy.manager.growth.presentation.dto.WeeklyCalendarResponse;
import com.ssafy.manager.member.application.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class GrowthController {

    private final DailyGoalSummaryService dailyGoalSummaryService;
    private final MemberService memberService;

    @GetMapping("/growth/weekly-calendar")
    public ResponseEntity<WeeklyCalendarResponse> weeklyCalendar(
            @AuthenticationPrincipal Long memberId) {
        WeeklyAchievementSummary summary = dailyGoalSummaryService.weeklyCalendar(memberId, LocalDate.now());
        return ResponseEntity.ok(WeeklyCalendarResponse.from(summary));
    }

    @GetMapping("/growth/progress")
    public ResponseEntity<DailyProgress> progress(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(dailyGoalSummaryService.todayProgress(memberId, LocalDate.now()));
    }

    @GetMapping("/nyam/status")
    public ResponseEntity<NyamStatusResponse> nyamStatus(
            @AuthenticationPrincipal Long memberId) {
        DailyProgress progress = dailyGoalSummaryService.todayProgress(memberId, LocalDate.now());
        var member = memberService.getMember(memberId);
        return ResponseEntity.ok(NyamStatusResponse.of(progress.achievementRate(), member.healthGoal()));
    }
}
