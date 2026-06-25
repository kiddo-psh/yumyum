package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.application.WeeklyReportQueryService;
import com.ssafy.manager.program.presentation.dto.WeeklyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportQueryService weeklyReportQueryService;

    @GetMapping("/{programId}/weekly-reports/{weekNumber}")
    public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long programId,
            @PathVariable int weekNumber
    ) {
        WeeklyReportResponse response =
                WeeklyReportResponse.from(weeklyReportQueryService.getReport(memberId, programId, weekNumber));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{programId}/weekly-reports")
    public ResponseEntity<List<WeeklyReportResponse>> getWeeklyReports(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long programId
    ) {
        List<WeeklyReportResponse> responses = weeklyReportQueryService.getReports(memberId, programId)
                .stream()
                .map(WeeklyReportResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
