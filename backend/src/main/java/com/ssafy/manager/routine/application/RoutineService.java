package com.ssafy.manager.routine.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.SplitType;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClientRequest;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClientResponse;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final MemberRepository memberRepository;
    private final ProgramRepository programRepository;
    private final RoutineRepository routineRepository;
    private final RoutineExerciseRepository routineExerciseRepository;
    private final AiRoutineClient aiRoutineClient;

    @Transactional
    public RoutineResult createAi(Long memberId, int daysPerWeek, SplitType splitType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        String healthGoal = programRepository
                .findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .map(p -> toHealthGoal(p.getType()))
                .orElse("MAINTAIN");

        AiRoutineClientRequest request = new AiRoutineClientRequest(
                member.getSex() == Sex.MALE ? "M" : "F",
                member.age(LocalDate.now().getYear()),
                member.getWeightKg(),
                member.getHeightCm(),
                healthGoal,
                daysPerWeek,
                splitType.name(),
                splitType.getSplitLabels()
        );

        AiRoutineClientResponse response = aiRoutineClient.generate(request);

        Routine routine = Routine.create(memberId, response.routineName(), daysPerWeek, true);
        routineRepository.save(routine);

        List<RoutineExercise> exercises = new ArrayList<>();
        for (AiRoutineClientResponse.Day day : response.days()) {
            for (int i = 0; i < day.exercises().size(); i++) {
                AiRoutineClientResponse.Exercise ex = day.exercises().get(i);
                exercises.add(RoutineExercise.create(
                        routine.getId(), day.dayLabel(), ex.name(),
                        ex.sets(), ex.reps(), ex.weightKg(), i
                ));
            }
        }
        routineExerciseRepository.saveAll(exercises);

        return RoutineResult.from(routine, exercises, response.aiComment());
    }

    @Transactional
    public RoutineResult createManual(Long memberId, String name, int daysPerWeek,
                                      List<ExerciseInput> inputs) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        Routine routine = Routine.create(memberId, name, daysPerWeek, false);
        routineRepository.save(routine);

        List<RoutineExercise> exercises = inputs.stream()
                .map(e -> RoutineExercise.create(routine.getId(), e.dayLabel(), e.exerciseName(),
                        e.targetSets(), e.targetReps(), e.targetWeightKg(), e.orderIndex()))
                .toList();
        routineExerciseRepository.saveAll(exercises);

        return RoutineResult.from(routine, exercises, null);
    }

    @Transactional
    public RoutineResult.ExerciseResult updateExercise(Long routineId, Long exerciseId,
                                                       String exerciseName, int targetSets,
                                                       int targetReps, double targetWeightKg) {
        if (!routineRepository.existsById(routineId)) {
            throw new NoSuchElementException("루틴을 찾을 수 없습니다.");
        }
        RoutineExercise exercise = routineExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("운동을 찾을 수 없습니다."));
        exercise.update(exerciseName, targetSets, targetReps, targetWeightKg);
        return RoutineResult.ExerciseResult.from(exercise);
    }

    public List<RoutineResult.ExerciseResult> getWeeklyPlan(Long routineId, int week) {
        return routineExerciseRepository
                .findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(routineId, week)
                .stream()
                .map(RoutineResult.ExerciseResult::from)
                .toList();
    }

    private String toHealthGoal(ProgramType type) {
        return switch (type) {
            case DIET -> "WEIGHT_LOSS";
            case MUSCLE -> "MUSCLE_GAIN";
            case HEALTH, DISEASE -> "MAINTAIN";
        };
    }
}
