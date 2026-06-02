package com.ssafy.manager.program.infrastructure.persistence;

import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    Optional<Program> findByMemberIdAndStatus(Long memberId, ProgramStatus status);
}
