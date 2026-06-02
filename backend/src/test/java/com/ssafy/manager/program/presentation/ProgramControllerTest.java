package com.ssafy.manager.program.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.program.application.ProgramService;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.presentation.dto.ProgramRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
class ProgramControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ProgramService programService;

    private static final Long MEMBER_ID = 1L;

    @Test
    void 프로그램을_생성하면_201을_반환한다() throws Exception {
        ProgramRequest request = new ProgramRequest(
                ProgramType.DIET,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        mockMvc.perform(post("/programs")
                        .header("X-Member-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(programService).create(MEMBER_ID, ProgramType.DIET,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
    }
}
