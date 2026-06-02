package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakResetService {

    private final MemberStatsRepository memberStatsRepository;

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
