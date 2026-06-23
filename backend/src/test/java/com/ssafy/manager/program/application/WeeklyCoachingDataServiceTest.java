package com.ssafy.manager.program.application;

import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.client.AiCoachingClientRequest;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import com.ssafy.manager.weight.domain.Weight;
import com.ssafy.manager.weight.infrastructure.persistence.WeightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WeeklyCoachingDataServiceTest {

    @Mock MealItemRepository mealItemRepository;
    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock SessionSetRepository sessionSetRepository;
    @Mock WeightRepository weightRepository;
    @Mock ProgramRepository programRepository;

    @InjectMocks WeeklyCoachingDataService service;

    private static final LocalDate START = LocalDate.of(2026, 6, 9);
    private static final Long PROGRAM_ID = 1L;

    @Test
    void week1_날짜_범위가_startDate부터_6일간이다() {
        Program program = Program.create(1L, ProgramType.MUSCLE, START, START.plusDays(27),
                2400, 120.0, 250.0, 60.0, null);

        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(program));
        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());
        given(routineSessionRepository.findDailyCaloriesBurnedByMemberIdAndDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(
                eq(1L), eq(START), eq(START.plusDays(6)))).willReturn(List.of());

        AiCoachingClientRequest request = service.buildRequest(PROGRAM_ID, 1);

        assertThat(request.weekNumber()).isEqualTo(1);
        assertThat(request.healthGoal()).isEqualTo("MUSCLE");
        assertThat(request.dailyNutrition()).hasSize(7);
        assertThat(request.dailyNutrition().get(0).date()).isEqualTo(START.toString());
        assertThat(request.dailyNutrition().get(6).date()).isEqualTo(START.plusDays(6).toString());
    }

    @Test
    void 체중_기록이_없으면_weightRecords가_빈_리스트다() {
        Program program = Program.create(1L, ProgramType.DIET, START, START.plusDays(27),
                1800, 90.0, 200.0, 50.0, null);

        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(program));
        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(routineSessionRepository.findDailyCaloriesBurnedByMemberIdAndDateBetween(any(), any(), any())).willReturn(List.of());
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(any(), any(), any()))
                .willReturn(List.of());

        AiCoachingClientRequest request = service.buildRequest(PROGRAM_ID, 1);

        assertThat(request.weightRecords()).isEmpty();
    }

    @Test
    void 체중_기록이_있으면_weightRecords에_포함된다() {
        Program program = Program.create(1L, ProgramType.DIET, START, START.plusDays(27),
                1800, 90.0, 200.0, 50.0, null);
        Weight w = Weight.create(1L, 72.5, START.plusDays(3));

        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(program));
        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(routineSessionRepository.findDailyCaloriesBurnedByMemberIdAndDateBetween(any(), any(), any())).willReturn(List.of());
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(any(), any(), any()))
                .willReturn(List.of(w));

        AiCoachingClientRequest request = service.buildRequest(PROGRAM_ID, 1);

        assertThat(request.weightRecords()).hasSize(1);
        assertThat(request.weightRecords().get(0).weightKg()).isEqualTo(72.5);
    }

    @Test
    void 운동_세션이_없으면_routineSessions가_빈_리스트다() {
        Program program = Program.create(1L, ProgramType.HEALTH, START, START.plusDays(27),
                2200, 100.0, 230.0, 55.0, null);

        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(program));
        given(mealItemRepository.findDailyNutritionByMemberIdAndDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(routineSessionRepository.findDailyCaloriesBurnedByMemberIdAndDateBetween(any(), any(), any())).willReturn(List.of());
        given(routineSessionRepository.findByMemberIdAndSessionDateBetween(any(), any(), any()))
                .willReturn(List.of());
        given(weightRepository.findByMemberIdAndRecordedDateBetween(any(), any(), any()))
                .willReturn(List.of());

        AiCoachingClientRequest request = service.buildRequest(PROGRAM_ID, 1);

        assertThat(request.routineSessions()).isEmpty();
    }
}
