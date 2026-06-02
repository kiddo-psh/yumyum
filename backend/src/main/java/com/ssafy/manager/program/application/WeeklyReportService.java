package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final ProgramRepository programRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    @Transactional
    public void createStubs(LocalDate today) {
        programRepository.findAllByStatus(ProgramStatus.ACTIVE).forEach(p -> {
            long daysElapsed = ChronoUnit.DAYS.between(p.getStartDate(), today);
            if (daysElapsed < 7) return;

            int weekNumber = (int) (daysElapsed / 7);
            if (!weeklyReportRepository.existsByProgramIdAndWeekNumber(p.getId(), weekNumber)) {
                weeklyReportRepository.save(new WeeklyReport(p.getId(), weekNumber));
            }
        });
    }
}
