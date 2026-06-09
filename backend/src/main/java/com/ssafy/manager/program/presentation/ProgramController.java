package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.application.ProgramResult;
import com.ssafy.manager.program.application.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    public ResponseEntity<CreateProgramResponse> create(
            @RequestBody CreateProgramRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        ProgramResult result = programService.create(
                request.memberId(),
                request.healthGoal(),
                request.startDate(),
                request.durationWeeks()
        );
        URI location = uriBuilder.path("/programs/{id}").buildAndExpand(result.programId()).toUri();
        return ResponseEntity.created(location).body(CreateProgramResponse.from(result));
    }

    @GetMapping("/current")
    public ResponseEntity<CreateProgramResponse> getCurrent(@RequestParam Long memberId) {
        // TODO: JWT 도입 후 @AuthenticationPrincipal로 교체
        ProgramResult result = programService.getCurrent(memberId);
        return ResponseEntity.ok(CreateProgramResponse.from(result));
    }
}
