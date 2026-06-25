package com.ssafy.manager.program.infrastructure.persistence;

import com.ssafy.manager.program.domain.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    boolean existsByProgramIdAndWeekNumber(Long programId, int weekNumber);
    Optional<WeeklyReport> findByProgramIdAndWeekNumber(Long programId, int weekNumber);
    List<WeeklyReport> findByProgramIdOrderByWeekNumberAsc(Long programId);
}
