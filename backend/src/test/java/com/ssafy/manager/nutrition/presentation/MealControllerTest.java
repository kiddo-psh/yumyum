package com.ssafy.manager.nutrition.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.nutrition.application.MealService;
import com.ssafy.manager.nutrition.domain.MealType;
import com.ssafy.manager.nutrition.presentation.dto.MealRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.manager.nutrition.domain.Meal;
import com.ssafy.manager.nutrition.domain.MealType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealController.class)
class MealControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MealService mealService;

    private static final Long MEMBER_ID = 1L;

    @Test
    void 식사를_기록하면_201을_반환한다() throws Exception {
        MealRequest request = new MealRequest(
                MealType.LUNCH,
                LocalDate.of(2026, 6, 1),
                List.of(new MealRequest.Item(1L, 100.0))
        );

        mockMvc.perform(post("/meals")
                        .header("X-Member-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(mealService).record(any(), any());
    }

    @Test
    void 날짜로_식사_목록을_조회한다() throws Exception {
        Meal meal = new Meal(MEMBER_ID, MealType.LUNCH, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
        given(mealService.listByDate(MEMBER_ID, LocalDate.of(2026, 6, 1)))
                .willReturn(List.of(meal));

        mockMvc.perform(get("/meals")
                        .header("X-Member-Id", MEMBER_ID)
                        .param("date", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("LUNCH"));
    }

    @Test
    void date_파라미터_없이_조회하면_오늘_기준으로_반환한다() throws Exception {
        given(mealService.listByDate(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/meals")
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isOk());
    }
}
