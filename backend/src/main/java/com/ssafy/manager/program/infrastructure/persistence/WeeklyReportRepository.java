package com.ssafy.manager.program.infrastructure.persistence;

import com.ssafy.manager.program.domain.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    boolean existsByProgramIdAndWeekNumber(Long programId, int weekNumber);
}
