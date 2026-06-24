package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.WeeklyReport;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClient;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientResponse;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final ProgramRepository programRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final WeeklyCoachingDataService weeklyCoachingDataService;
    private final AiCoachingClient aiCoachingClient;

    @Transactional
    public void createStubs(LocalDate today) {
        programRepository.findAllByStatus(ProgramStatus.ACTIVE).forEach(p -> {
            long daysElapsed = ChronoUnit.DAYS.between(p.getStartDate(), today);
            if (daysElapsed < 7) return;

            int weekNumber = (int) (daysElapsed / 7);
            if (!weeklyReportRepository.existsByProgramIdAndWeekNumber(p.getId(), weekNumber)) {
                WeeklyReport stub = weeklyReportRepository.save(new WeeklyReport(p.getId(), weekNumber));
                fillWithAiContent(stub, p.getId(), weekNumber);
            }
        });
    }

    private void fillWithAiContent(WeeklyReport stub, Long programId, int weekNumber) {
        try {
            AiCoachingClientRequest request = weeklyCoachingDataService.buildRequest(programId, weekNumber);
            AiCoachingClientResponse response = aiCoachingClient.weekly(request);
            stub.fill(
                    response.aiComment(),
                    response.nutritionSummary(),
                    response.exerciseSummary(),
                    response.goalSummary(),
                    response.avgCalorieRate(),
                    response.achievementDays(),
                    response.weightTrend()
            );
            weeklyReportRepository.save(stub);
        } catch (Exception e) {
            log.warn("주간 코칭 AI 호출 실패 — programId={}, week={}: {}",
                    programId, weekNumber, e.getMessage());
        }
    }
}
