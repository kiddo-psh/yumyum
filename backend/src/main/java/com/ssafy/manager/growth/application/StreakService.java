package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final MemberStatsRepository memberStatsRepository;

    @Transactional
    public void increment(Long memberId, LocalDate achievedDate) {
        MemberStats stats = memberStatsRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException());

        stats.incrementStreak(achievedDate);
        memberStatsRepository.save(stats);
    }
}
