package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.ExerciseInput;
import com.ssafy.manager.routine.application.RoutineResult;
import com.ssafy.manager.routine.application.RoutineService;
import com.ssafy.manager.routine.domain.SplitType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @GetMapping("/split-options")
    public List<SplitOptionResponse> getSplitOptions(@RequestParam int daysPerWeek) {
        return SplitType.findByDaysPerWeek(daysPerWeek).stream()
                .map(s -> new SplitOptionResponse(s.name(), s.getLabel()))
                .toList();
    }

    @PostMapping("/ai")
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse createAi(@RequestBody CreateAiRoutineRequest request) {
        RoutineResult result = routineService.createAi(
                request.memberId(), request.daysPerWeek(), request.splitType());
        return RoutineResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse createManual(@RequestBody CreateManualRoutineRequest request) {
        List<ExerciseInput> inputs = request.exercises().stream()
                .map(e -> new ExerciseInput(e.dayLabel(), e.exerciseName(),
                        e.targetSets(), e.targetReps(), e.targetWeightKg(), e.orderIndex()))
                .toList();
        RoutineResult result = routineService.createManual(
                request.memberId(), request.name(), request.daysPerWeek(), inputs);
        return RoutineResponse.from(result);
    }

    @PatchMapping("/{routineId}/exercises/{exerciseId}")
    public RoutineResponse.ExerciseResponse updateExercise(
            @PathVariable Long routineId,
            @PathVariable Long exerciseId,
            @RequestBody UpdateRoutineExerciseRequest request) {
        RoutineResult.ExerciseResult result = routineService.updateExercise(
                routineId, exerciseId,
                request.exerciseName(), request.targetSets(),
                request.targetReps(), request.targetWeightKg());
        return RoutineResponse.ExerciseResponse.from(result);
    }

    @GetMapping("/{routineId}/weekly-plan/{week}")
    public List<RoutineResponse.ExerciseResponse> getWeeklyPlan(
            @PathVariable Long routineId,
            @PathVariable int week) {
        return routineService.getWeeklyPlan(routineId, week).stream()
                .map(RoutineResponse.ExerciseResponse::from)
                .toList();
    }

    private record SplitOptionResponse(String splitType, String label) {}
}
