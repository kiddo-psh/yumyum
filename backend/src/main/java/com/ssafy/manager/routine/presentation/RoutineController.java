package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.ExerciseInput;
import com.ssafy.manager.routine.application.RoutineResult;
import com.ssafy.manager.routine.application.RoutineService;
import com.ssafy.manager.routine.domain.SplitType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @GetMapping
    public ResponseEntity<List<RoutineSummaryResponse>> getMyRoutines(
            @AuthenticationPrincipal Long memberId) {
        List<RoutineSummaryResponse> body = routineService.getMyRoutines(memberId).stream()
                .map(RoutineSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/split-options")
    public ResponseEntity<List<SplitOptionResponse>> getSplitOptions(@RequestParam int daysPerWeek) {
        List<SplitOptionResponse> body = SplitType.findByDaysPerWeek(daysPerWeek).stream()
                .map(s -> new SplitOptionResponse(s.name(), s.getLabel()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/ai")
    public ResponseEntity<RoutineResponse> createAi(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CreateAiRoutineRequest request) {
        RoutineResult result = routineService.createAi(
                memberId, request.hasExistingRoutine(), request.daysPerWeek(), request.splitType());
        return ResponseEntity.status(HttpStatus.CREATED).body(RoutineResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<RoutineResponse> createManual(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CreateManualRoutineRequest request) {
        List<ExerciseInput> inputs = request.exercises().stream()
                .map(e -> new ExerciseInput(e.dayLabel(), e.exerciseName(),
                        e.targetSets(), e.targetReps(), e.targetWeightKg(), e.orderIndex()))
                .toList();
        RoutineResult result = routineService.createManual(
                memberId, request.name(), request.daysPerWeek(), inputs);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoutineResponse.from(result));
    }

    @PatchMapping("/{routineId}/exercises/{exerciseId}")
    public ResponseEntity<RoutineResponse.ExerciseResponse> updateExercise(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long routineId,
            @PathVariable Long exerciseId,
            @RequestBody UpdateRoutineExerciseRequest request) {
        RoutineResult.ExerciseResult result = routineService.updateExercise(
                memberId, routineId, exerciseId,
                request.exerciseName(), request.targetSets(),
                request.targetReps(), request.targetWeightKg());
        return ResponseEntity.ok(RoutineResponse.ExerciseResponse.from(result));
    }

    @GetMapping("/{routineId}/weekly-plan/{week}")
    public ResponseEntity<List<RoutineResponse.ExerciseResponse>> getWeeklyPlan(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long routineId,
            @PathVariable int week) {
        List<RoutineResponse.ExerciseResponse> body = routineService.getWeeklyPlan(memberId, routineId, week).stream()
                .map(RoutineResponse.ExerciseResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    private record SplitOptionResponse(String splitType, String label) {}
}
