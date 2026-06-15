package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineSessionService;
import com.ssafy.manager.routine.application.SessionSetInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineSessionController {

    private final RoutineSessionService routineSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> recordSession(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CreateSessionRequest request) {
        List<SessionSetInput> inputs = request.sets().stream()
                .map(s -> new SessionSetInput(s.exerciseId(), s.exerciseName(),
                        s.setNumber(), s.actualReps(), s.actualWeightKg(), s.completed()))
                .toList();
        SessionResponse body = SessionResponse.from(routineSessionService.recordSession(
                memberId, request.routineId(), request.sessionDate(),
                request.caloriesBurned(), inputs));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
