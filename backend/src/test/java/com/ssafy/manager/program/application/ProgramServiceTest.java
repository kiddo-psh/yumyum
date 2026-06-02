package com.ssafy.manager.program.application;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock ProgramRepository programRepository;

    @InjectMocks ProgramService programService;

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final LocalDate END = LocalDate.of(2026, 6, 28);

    // 남성, 1990년생(36세), 175cm, 80kg, 보통활동 → TDEE=2664
    private final Member member = new Member(Sex.MALE, 1990, 175.0, 80.0, ActivityLevel.MODERATELY_ACTIVE);

    @Test
    void Program_생성_시_TDEE_기반_targetCalories로_저장된다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(programRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        programService.create(MEMBER_ID, ProgramType.HEALTH, START, END);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetCalories()).isEqualTo(2664);
    }

    @Test
    void DIET_타입이면_targetCalories가_TDEE에서_500_차감된다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(programRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        programService.create(MEMBER_ID, ProgramType.DIET, START, END);

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetCalories()).isEqualTo(2164); // 2664 - 500
    }

    @Test
    void 활성_Program이_이미_있으면_예외가_발생한다() {
        Program activeProgram = Program.create(MEMBER_ID, ProgramType.HEALTH, START, END, 2664);
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.of(activeProgram));

        assertThatThrownBy(() -> programService.create(MEMBER_ID, ProgramType.DIET, START, END))
                .isInstanceOf(IllegalStateException.class);
    }
}
