package com.ssafy.manager.nutrition.presentation;

import com.ssafy.manager.nutrition.application.DailySummaryService;
import com.ssafy.manager.nutrition.presentation.dto.DailySummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DailySummaryController.class)
class DailySummaryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DailySummaryService dailySummaryService;

    private static final Long MEMBER_ID = 1L;

    @Test
    void PRD_명세_구조로_일일_요약을_반환한다() throws Exception {
        DailySummaryResponse response = new DailySummaryResponse(
                2664, 1200.0, false, 5, 10, 120.0, 80.0, 40.0
        );
        given(dailySummaryService.getSummary(MEMBER_ID, LocalDate.of(2026, 6, 1)))
                .willReturn(response);

        mockMvc.perform(get("/daily-summary")
                        .header("X-Member-Id", MEMBER_ID)
                        .param("date", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCalories").value(2664))
                .andExpect(jsonPath("$.achievedCalories").value(1200.0))
                .andExpect(jsonPath("$.achieved").value(false))
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.maxStreak").value(10))
                .andExpect(jsonPath("$.totalCarbs").value(120.0))
                .andExpect(jsonPath("$.totalProtein").value(80.0))
                .andExpect(jsonPath("$.totalFat").value(40.0));
    }
}
