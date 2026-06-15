package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.domain.MemberStats;
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
        List<MemberStats> expiredStats = memberStatsRepository.findAllWithExpiredStreak(date.minusDays(1));
        if (expiredStats.isEmpty()) return;
        expiredStats.forEach(MemberStats::resetStreak);
        memberStatsRepository.saveAll(expiredStats);
    }

    @Transactional
    public void resetFor(List<Long> memberIds) {
        List<MemberStats> statsList = memberStatsRepository.findAllByMemberIdIn(memberIds);
        if (statsList.isEmpty()) return;
        statsList.forEach(MemberStats::resetStreak);
        memberStatsRepository.saveAll(statsList);
    }
}
