package com.ssafy.manager.routine.application;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.SplitType;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClientResponse;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock ProgramRepository programRepository;
    @Mock RoutineRepository routineRepository;
    @Mock RoutineExerciseRepository routineExerciseRepository;
    @Mock AiRoutineClient aiRoutineClient;

    @InjectMocks RoutineService routineService;

    private static final Long MEMBER_ID = 1L;
    private final Member member = createOnboardedMember();

    private static Member createOnboardedMember() {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        member.completeOnboarding(Sex.MALE, 1995, 178.0, 75.0, ActivityLevel.MODERATELY_ACTIVE, HealthGoal.MUSCLE);
        return member;
    }

    private static final AiRoutineClientResponse AI_RESPONSE = new AiRoutineClientResponse(
            "4일 상체/하체 분할 루틴",
            List.of(
                    new AiRoutineClientResponse.Day("상체", List.of(
                            new AiRoutineClientResponse.Exercise("벤치프레스", 4, 8, 60.0)
                    )),
                    new AiRoutineClientResponse.Day("하체", List.of(
                            new AiRoutineClientResponse.Exercise("바벨 스쿼트", 4, 8, 80.0)
                    ))
            ),
            "열심히 해봐요!"
    );

    @Test
    void AI_루틴_생성시_Routine과_RoutineExercise가_저장된다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(aiRoutineClient.generate(any())).willReturn(AI_RESPONSE);

        RoutineResult result = routineService.createAi(MEMBER_ID, 4, SplitType.UPPER_LOWER_4);

        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(routineCaptor.capture());
        assertThat(routineCaptor.getValue().isAiGenerated()).isTrue();
        assertThat(routineCaptor.getValue().getDaysPerWeek()).isEqualTo(4);

        verify(routineExerciseRepository).saveAll(any());
        assertThat(result.aiComment()).isEqualTo("열심히 해봐요!");
        assertThat(result.exercises()).isNotEmpty();
    }

    @Test
    void 수동_루틴_생성시_FastAPI를_호출하지_않는다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        routineService.createManual(MEMBER_ID, "내 루틴", 3, List.of(
                new ExerciseInput("상체", "벤치프레스", 4, 8, 60.0, 0)
        ));

        verifyNoInteractions(aiRoutineClient);
        verify(routineRepository).save(any());
        verify(routineExerciseRepository).saveAll(any());
    }

    @Test
    void 없는_회원으로_AI_루틴_생성시_예외가_발생한다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.createAi(MEMBER_ID, 4, SplitType.UPPER_LOWER_4))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다");
    }

    @Test
    void 운동_수정시_필드가_변경된다() {
        Routine routine = Routine.create(MEMBER_ID, "내 루틴", 4, false);
        RoutineExercise exercise = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);
        given(routineRepository.findById(1L)).willReturn(Optional.of(routine));
        given(routineExerciseRepository.findById(1L)).willReturn(Optional.of(exercise));

        RoutineResult.ExerciseResult result =
                routineService.updateExercise(MEMBER_ID, 1L, 1L, "인클라인 벤치프레스", 3, 10, 55.0);

        assertThat(result.exerciseName()).isEqualTo("인클라인 벤치프레스");
        assertThat(result.targetSets()).isEqualTo(3);
        assertThat(result.targetWeightKg()).isEqualTo(55.0);
    }

    @Test
    void 없는_루틴_수정시_예외가_발생한다() {
        given(routineRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.updateExercise(MEMBER_ID, 99L, 1L, "벤치프레스", 4, 8, 60.0))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("루틴을 찾을 수 없습니다");
    }
}
