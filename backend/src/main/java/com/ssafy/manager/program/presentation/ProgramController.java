package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.application.ProgramService;
import com.ssafy.manager.program.presentation.dto.ProgramRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody ProgramRequest request
    ) {
        programService.create(memberId, request.type(), request.startDate(), request.endDate());
    }
}
