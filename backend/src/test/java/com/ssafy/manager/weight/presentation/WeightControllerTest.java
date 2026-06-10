package com.ssafy.manager.weight.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.weight.application.WeightService;
import com.ssafy.manager.weight.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeightController.class)
@AutoConfigureMockMvc(addFilters = false)
class WeightControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean WeightService weightService;

    @Test
    void 체중_등록_성공시_201_반환() throws Exception {
        Weight saved = Weight.create(1L, 72.5, LocalDate.of(2026, 6, 10));
        given(weightService.record(any(), anyDouble(), any())).willReturn(saved);

        String body = """
                {"memberId": 1, "weightKg": 72.5, "recordedDate": "2026-06-10"}
                """;
        mockMvc.perform(post("/weights").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg").value(72.5))
                .andExpect(jsonPath("$.recordedDate").value("2026-06-10"));
    }

    @Test
    void 체중_목록_조회_성공시_200_반환() throws Exception {
        Weight w = Weight.create(1L, 72.5, LocalDate.of(2026, 6, 10));
        given(weightService.findByMember(1L)).willReturn(List.of(w));

        mockMvc.perform(get("/weights").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].weightKg").value(72.5));
    }

    @Test
    void 체중_삭제_성공시_204_반환() throws Exception {
        mockMvc.perform(delete("/weights/1").param("memberId", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 없는_체중_삭제시_404_반환() throws Exception {
        willThrow(new NoSuchElementException("체중 기록을 찾을 수 없습니다."))
                .given(weightService).delete(eq(1L), eq(99L));

        mockMvc.perform(delete("/weights/99").param("memberId", "1"))
                .andExpect(status().isNotFound());
    }
}
