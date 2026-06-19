package com.ssafy.manager.program.application;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.OnboardingRequiredException;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.client.AiPlanClient;
import com.ssafy.manager.program.infrastructure.client.AiPlanClientResponse;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock ProgramRepository programRepository;
    @Mock AiPlanClient aiPlanClient;

    @InjectMocks ProgramService programService;

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final int WEEKS = 4;

    private static final AiPlanClientResponse AI_RESPONSE = new AiPlanClientResponse(
            1700.0, 2164.0, 1800.0, 135.0, 202.5, 50.0, "열심히 해봐요!"
    );

    private Member onboardedMember(HealthGoal goal) {
        Member member = new Member("kakao", "12345", "test@kakao.com");
        member.completeOnboarding(Sex.MALE, 1990, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE, goal);
        return member;
    }

    @Test
    void FastAPI_결과로_Program이_저장된다() {
        Member member = onboardedMember(HealthGoal.HEALTH);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(aiPlanClient.generate(member, ProgramType.HEALTH)).willReturn(AI_RESPONSE);

        programService.create(MEMBER_ID, START, WEEKS);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        Program saved = captor.getValue();

        assertThat(saved.getTargetCalories()).isEqualTo(1800);
        assertThat(saved.getTargetProteinG()).isEqualTo(135.0);
        assertThat(saved.getTargetCarbG()).isEqualTo(202.5);
        assertThat(saved.getTargetFatG()).isEqualTo(50.0);
        assertThat(saved.getAiComment()).isEqualTo("열심히 해봐요!");
    }

    @Test
    void healthGoal에_따라_ProgramType이_결정된다() {
        Member member = onboardedMember(HealthGoal.DIET);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(aiPlanClient.generate(member, ProgramType.DIET)).willReturn(AI_RESPONSE);

        programService.create(MEMBER_ID, START, WEEKS);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(ProgramType.DIET);
    }

    @Test
    void endDate는_startDate에서_durationWeeks를_더한_날짜다() {
        Member member = onboardedMember(HealthGoal.DIET);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(aiPlanClient.generate(member, ProgramType.DIET)).willReturn(AI_RESPONSE);

        programService.create(MEMBER_ID, START, WEEKS);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isEqualTo(START.plusWeeks(WEEKS));
    }

    @Test
    void ProgramResult에_durationWeeks와_AI_값이_담긴다() {
        Member member = onboardedMember(HealthGoal.MUSCLE);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(aiPlanClient.generate(member, ProgramType.MUSCLE)).willReturn(AI_RESPONSE);

        ProgramResult result = programService.create(MEMBER_ID, START, WEEKS);

        assertThat(result.durationWeeks()).isEqualTo(WEEKS);
        assertThat(result.dailyKcal()).isEqualTo(1800);
        assertThat(result.aiComment()).isEqualTo("열심히 해봐요!");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void 온보딩_미완료_회원이면_예외가_발생한다() {
        Member notOnboarded = new Member("kakao", "12345", "test@kakao.com");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(notOnboarded));

        assertThatThrownBy(() -> programService.create(MEMBER_ID, START, WEEKS))
                .isInstanceOf(OnboardingRequiredException.class);
    }

    @Test
    void 활성_Program이_이미_있으면_예외가_발생한다() {
        Member member = onboardedMember(HealthGoal.DIET);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        Program activeProgram = Program.create(MEMBER_ID, ProgramType.HEALTH, START,
                START.plusWeeks(WEEKS), 2664, 0, 0, 0, null);
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.of(activeProgram));

        assertThatThrownBy(() -> programService.create(MEMBER_ID, START, WEEKS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 활성화된");
    }
}
