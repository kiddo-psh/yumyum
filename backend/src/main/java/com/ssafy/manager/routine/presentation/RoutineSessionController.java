package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineSessionService;
import com.ssafy.manager.routine.application.SessionSetInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineSessionController {

    private final RoutineSessionService routineSessionService;

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse recordSession(@RequestBody CreateSessionRequest request) {
        List<SessionSetInput> inputs = request.sets().stream()
                .map(s -> new SessionSetInput(s.exerciseId(), s.exerciseName(),
                        s.setNumber(), s.actualReps(), s.actualWeightKg(), s.completed()))
                .toList();
        return SessionResponse.from(routineSessionService.recordSession(
                request.memberId(), request.routineId(), request.sessionDate(), inputs));
    }
}
