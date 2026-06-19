package com.ssafy.manager.program.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.member.domain.OnboardingRequiredException;
import com.ssafy.manager.program.application.ProgramResult;
import com.ssafy.manager.program.application.ProgramService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgramControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ProgramService programService;

    private static final ProgramResult RESULT = new ProgramResult(
            1L,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 29),
            4,
            1800,
            135.0,
            202.5,
            50.0,
            "열심히 해봐요!",
            "ACTIVE"
    );

    @Test
    void 프로그램_생성_성공시_201과_응답_본문_반환() throws Exception {
        given(programService.create(any(), any(), anyInt())).willReturn(RESULT);

        CreateProgramRequest request = new CreateProgramRequest(
                1L, LocalDate.of(2026, 6, 1), 4
        );

        mockMvc.perform(post("/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/programs/1"))
                .andExpect(jsonPath("$.programId").value(1))
                .andExpect(jsonPath("$.dailyKcal").value(1800))
                .andExpect(jsonPath("$.targetProtein").value(135.0))
                .andExpect(jsonPath("$.targetCarb").value(202.5))
                .andExpect(jsonPath("$.targetFat").value(50.0))
                .andExpect(jsonPath("$.aiComment").value("열심히 해봐요!"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void 활성_프로그램_중복시_409_반환() throws Exception {
        given(programService.create(any(), any(), anyInt()))
                .willThrow(new IllegalStateException("이미 활성화된 Program이 있습니다."));

        CreateProgramRequest request = new CreateProgramRequest(
                1L, LocalDate.of(2026, 6, 1), 4
        );

        mockMvc.perform(post("/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void 온보딩_미완료시_409와_해소경로_반환() throws Exception {
        given(programService.create(any(), any(), anyInt()))
                .willThrow(new OnboardingRequiredException(
                        "프로필 정보가 없어 프로그램을 생성할 수 없습니다. 온보딩을 먼저 완료해 주세요."));

        CreateProgramRequest request = new CreateProgramRequest(
                1L, LocalDate.of(2026, 6, 1), 4
        );

        mockMvc.perform(post("/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"))
                .andExpect(jsonPath("$.resolution.method").value("PATCH"))
                .andExpect(jsonPath("$.resolution.href").value("/members/me"));
    }

    @Test
    void 현재_프로그램_없으면_404_반환() throws Exception {
        given(programService.getCurrent(any()))
                .willThrow(new NoSuchElementException("활성화된 Program이 없습니다."));

        mockMvc.perform(get("/programs/current")
                        .param("memberId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void 현재_프로그램_조회_성공() throws Exception {
        given(programService.getCurrent(1L)).willReturn(RESULT);

        mockMvc.perform(get("/programs/current")
                        .param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
