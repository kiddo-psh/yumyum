package com.ssafy.manager.program.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.domain.TdeeCalculator;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final MemberRepository memberRepository;
    private final ProgramRepository programRepository;

    @Transactional
    public Long create(Long memberId, ProgramType type, LocalDate startDate, LocalDate endDate) {
        programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new IllegalStateException("이미 활성화된 Program이 있습니다."); });

        Member member = memberRepository.findById(memberId).orElseThrow();
        int currentYear = LocalDate.now().getYear();
        int tdee = TdeeCalculator.calculate(member.getSex(), member.age(currentYear),
                member.getHeightCm(), member.getWeightKg(), member.getActivityLevel());
        int targetCalories = type.adjust(tdee);

        Program program = programRepository.save(Program.create(memberId, type, startDate, endDate, targetCalories));
        return program.getId();
    }
}
