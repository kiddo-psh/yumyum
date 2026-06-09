package com.ssafy.manager.routine.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.routine.application.RoutineResult;
import com.ssafy.manager.routine.application.RoutineService;
import com.ssafy.manager.routine.domain.SplitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutineController.class)
class RoutineControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RoutineService routineService;

    private static final RoutineResult RESULT = new RoutineResult(
            1L, "4일 상체/하체 분할 루틴", 4, true,
            List.of(new RoutineResult.ExerciseResult(1L, "상체", "벤치프레스", 4, 8, 60.0, 0)),
            "열심히 해봐요!"
    );

    @Test
    void AI_루틴_생성_성공시_201_반환() throws Exception {
        given(routineService.createAi(anyLong(), anyInt(), any())).willReturn(RESULT);

        mockMvc.perform(post("/routines/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAiRoutineRequest(1L, 4, SplitType.UPPER_LOWER_4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routineId").value(1))
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.aiComment").value("열심히 해봐요!"));
    }

    @Test
    void 수동_루틴_생성_성공시_201_반환() throws Exception {
        RoutineResult manualResult = new RoutineResult(
                2L, "내 루틴", 3, false,
                List.of(new RoutineResult.ExerciseResult(2L, "상체", "벤치프레스", 4, 8, 70.0, 0)),
                null
        );
        given(routineService.createManual(anyLong(), anyString(), anyInt(), any()))
                .willReturn(manualResult);

        String body = """
                {
                  "memberId": 1,
                  "name": "내 루틴",
                  "daysPerWeek": 3,
                  "exercises": [
                    {"dayLabel":"상체","exerciseName":"벤치프레스","targetSets":4,"targetReps":8,"targetWeightKg":70.0,"orderIndex":0}
                  ]
                }
                """;

        mockMvc.perform(post("/routines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routineId").value(2))
                .andExpect(jsonPath("$.aiGenerated").value(false));
    }

    @Test
    void split_options_조회_성공() throws Exception {
        mockMvc.perform(get("/routines/split-options").param("daysPerWeek", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].splitType").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    void 운동_수정_성공시_200_반환() throws Exception {
        RoutineResult.ExerciseResult updated = new RoutineResult.ExerciseResult(
                1L, "상체", "인클라인 벤치프레스", 3, 10, 55.0, 0
        );
        given(routineService.updateExercise(anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyDouble()))
                .willReturn(updated);

        mockMvc.perform(patch("/routines/1/exercises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateRoutineExerciseRequest("인클라인 벤치프레스", 3, 10, 55.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseName").value("인클라인 벤치프레스"))
                .andExpect(jsonPath("$.targetSets").value(3));
    }

    @Test
    void 없는_루틴_수정시_404_반환() throws Exception {
        given(routineService.updateExercise(anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyDouble()))
                .willThrow(new NoSuchElementException("루틴을 찾을 수 없습니다."));

        mockMvc.perform(patch("/routines/99/exercises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateRoutineExerciseRequest("벤치프레스", 4, 8, 60.0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
