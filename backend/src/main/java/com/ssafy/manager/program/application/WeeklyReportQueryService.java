package com.ssafy.manager.program.application;

import com.ssafy.manager.global.exception.ForbiddenException;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class WeeklyReportQueryService {

    private final ProgramRepository programRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    @Transactional(readOnly = true)
    public WeeklyReportResult getReport(Long memberId, Long programId, int weekNumber) {
        verifyOwnership(programId, memberId);
        return weeklyReportRepository.findByProgramIdAndWeekNumber(programId, weekNumber)
                .map(WeeklyReportResult::from)
                .orElseThrow(() -> new NoSuchElementException("WeeklyReport를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<WeeklyReportResult> getReports(Long memberId, Long programId) {
        verifyOwnership(programId, memberId);
        return weeklyReportRepository.findByProgramIdOrderByWeekNumberAsc(programId)
                .stream()
                .map(WeeklyReportResult::from)
                .toList();
    }

    private void verifyOwnership(Long programId, Long memberId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NoSuchElementException("Program을 찾을 수 없습니다."));
        if (!program.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }
    }
}
