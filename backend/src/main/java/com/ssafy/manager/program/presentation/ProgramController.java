package com.ssafy.manager.program.presentation;

import com.ssafy.manager.program.application.ProgramService;
import com.ssafy.manager.program.presentation.dto.ProgramRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody ProgramRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Long programId = programService.create(memberId, request.type(), request.startDate(), request.endDate());
        return ResponseEntity.created(
                uriBuilder.path("/programs/{id}").buildAndExpand(programId).toUri()
        ).build();
    }
}
