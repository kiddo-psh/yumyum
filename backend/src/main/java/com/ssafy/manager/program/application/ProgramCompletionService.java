package com.ssafy.manager.program.application;

import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProgramCompletionService {

    private final ProgramRepository programRepository;

    @Transactional
    public void completeExpired(LocalDate today) {
        programRepository.findAllByStatusAndEndDateBefore(ProgramStatus.ACTIVE, today)
                .forEach(p -> {
                    p.complete();
                    programRepository.save(p);
                });
    }
}
