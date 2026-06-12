package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakResetService {

    private final MemberStatsRepository memberStatsRepository;

    @Transactional
    public void resetUnachievedFor(LocalDate date) {
        // TODO: 회원 수가 대용량으로 늘어나면 Spring Batch 청크 처리로 교체 필요
        memberStatsRepository.findAllWithExpiredStreak(date.minusDays(1)).forEach(stats -> {
            stats.resetStreak();
            memberStatsRepository.save(stats);
        });
    }

    @Transactional
    public void resetFor(List<Long> memberIds) {
        memberIds.forEach(memberId ->
                memberStatsRepository.findByMemberId(memberId)
                        .ifPresent(stats -> {
                            stats.resetStreak();
                            memberStatsRepository.save(stats);
                        })
        );
    }
}
