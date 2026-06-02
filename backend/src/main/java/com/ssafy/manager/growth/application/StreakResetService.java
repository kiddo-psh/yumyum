package com.ssafy.manager.growth.application;

import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakResetService {

    private final MemberStatsRepository memberStatsRepository;
    private final DailyGoalRepository dailyGoalRepository;

    @Transactional
    public void resetUnachievedFor(LocalDate date) {
        List<Long> unachievedIds = dailyGoalRepository.findAllByDate(date).stream()
                .filter(g -> !g.isAchieved())
                .map(g -> g.getMemberId())
                .toList();
        resetFor(unachievedIds);
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
