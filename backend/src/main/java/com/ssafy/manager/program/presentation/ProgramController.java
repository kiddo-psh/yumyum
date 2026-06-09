package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.application.ProgramResult;
import com.ssafy.manager.program.application.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProgramResponse create(@RequestBody CreateProgramRequest request) {
        ProgramResult result = programService.create(
                request.memberId(),
                request.healthGoal(),
                request.startDate(),
                request.durationWeeks()
        );
        return CreateProgramResponse.from(result);
    }

    @GetMapping("/current")
    public CreateProgramResponse getCurrent(@RequestParam Long memberId) {
        // TODO: JWT 도입 후 @AuthenticationPrincipal로 교체
        ProgramResult result = programService.getCurrent(memberId);
        return CreateProgramResponse.from(result);
    }
}
