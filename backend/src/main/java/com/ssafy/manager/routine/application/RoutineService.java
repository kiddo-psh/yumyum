package com.ssafy.manager.routine.application;

import com.ssafy.manager.global.exception.ForbiddenException;
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

    public List<RoutineSummaryResult> getMyRoutines(Long memberId) {
        return routineRepository.findByMemberId(memberId).stream()
                .map(RoutineSummaryResult::from)
                .toList();
    }

    public RoutineResult getRoutine(Long memberId, Long routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new NoSuchElementException("루틴을 찾을 수 없습니다."));
        if (!routine.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 루틴만 조회할 수 있습니다.");
        }
        int maxWeek = routineExerciseRepository.findMaxWeekNumberByRoutineId(routineId);
        List<RoutineExercise> exercises =
                routineExerciseRepository.findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(
                        routineId, maxWeek);
        return RoutineResult.from(routine, exercises, null);
    }

    @Transactional
    public RoutineResult createAi(Long memberId, boolean hasExistingRoutine,
                                  int daysPerWeek, SplitType splitType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (!member.isOnboardingCompleted()) {
            throw new com.ssafy.manager.member.domain.OnboardingRequiredException("루틴 생성 전 온보딩을 완료해주세요.");
        }

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
                hasExistingRoutine,
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
    public RoutineResult.ExerciseResult addExercise(Long memberId, Long routineId,
                                                    String dayLabel, String exerciseName,
                                                    int targetSets, int targetReps, double targetWeightKg) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new NoSuchElementException("루틴을 찾을 수 없습니다."));
        if (!routine.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 루틴만 수정할 수 있습니다.");
        }
        List<RoutineExercise> existing =
                routineExerciseRepository.findByRoutineIdOrderByDayLabelAscOrderIndexAsc(routineId);
        int nextOrder = (int) existing.stream().filter(e -> e.getDayLabel().equals(dayLabel)).count();
        RoutineExercise exercise = RoutineExercise.create(
                routineId, dayLabel, exerciseName, targetSets, targetReps, targetWeightKg, nextOrder);
        routineExerciseRepository.save(exercise);
        return RoutineResult.ExerciseResult.from(exercise);
    }

    @Transactional
    public void deleteExercise(Long memberId, Long routineId, Long exerciseId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new NoSuchElementException("루틴을 찾을 수 없습니다."));
        if (!routine.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 루틴만 수정할 수 있습니다.");
        }
        RoutineExercise exercise = routineExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("운동을 찾을 수 없습니다."));
        routineExerciseRepository.delete(exercise);
    }

    @Transactional
    public RoutineResult.ExerciseResult updateExercise(Long memberId, Long routineId, Long exerciseId,
                                                       String exerciseName, int targetSets,
                                                       int targetReps, double targetWeightKg) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new NoSuchElementException("루틴을 찾을 수 없습니다."));
        if (!routine.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 루틴만 수정할 수 있습니다.");
        }
        RoutineExercise exercise = routineExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("운동을 찾을 수 없습니다."));
        exercise.update(exerciseName, targetSets, targetReps, targetWeightKg);
        return RoutineResult.ExerciseResult.from(exercise);
    }

    public List<RoutineResult.ExerciseResult> getWeeklyPlan(Long memberId, Long routineId, int week) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new NoSuchElementException("루틴을 찾을 수 없습니다."));
        if (!routine.getMemberId().equals(memberId)) {
            throw new ForbiddenException("본인의 루틴만 조회할 수 있습니다.");
        }
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
