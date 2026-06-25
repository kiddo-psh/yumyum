package com.ssafy.manager.program.presentation;

import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import com.ssafy.manager.program.presentation.dto.WeeklyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final ProgramRepository programRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    @GetMapping("/{programId}/weekly-reports/{weekNumber}")
    public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long programId,
            @PathVariable int weekNumber
    ) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NoSuchElementException("Program을 찾을 수 없습니다."));
        if (!program.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }
        WeeklyReport report = weeklyReportRepository.findByProgramIdAndWeekNumber(programId, weekNumber)
                .orElseThrow(() -> new NoSuchElementException("WeeklyReport를 찾을 수 없습니다."));
        return ResponseEntity.ok(WeeklyReportResponse.from(report));
    }

    @GetMapping("/{programId}/weekly-reports")
    public ResponseEntity<List<WeeklyReportResponse>> getWeeklyReports(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long programId
    ) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NoSuchElementException("Program을 찾을 수 없습니다."));
        if (!program.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }
        List<WeeklyReportResponse> reports = weeklyReportRepository
                .findByProgramIdOrderByWeekNumberAsc(programId)
                .stream()
                .map(WeeklyReportResponse::from)
                .toList();
        return ResponseEntity.ok(reports);
    }
}
