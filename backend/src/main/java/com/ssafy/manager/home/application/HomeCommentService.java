package com.ssafy.manager.home.application;

import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.home.infrastructure.client.AiHomeCommentClient;
import com.ssafy.manager.home.infrastructure.client.AiHomeCommentClientRequest;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeCommentService {

    private static final String CACHE_KEY_PREFIX = "home_comment:";
    private static final long CACHE_TTL_HOURS = 12;
    private static final String FALLBACK = "오늘도 건강한 하루 보내세요!";

    private final MemberRepository memberRepository;
    private final MemberStatsRepository memberStatsRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final MealItemRepository mealItemRepository;
    private final AiHomeCommentClient aiHomeCommentClient;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public String getComment(Long memberId) {
        String cacheKey = CACHE_KEY_PREFIX + memberId;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return cached;
        } catch (Exception e) {
            log.warn("[HomeCommentService] Redis 조회 실패: {}", e.getMessage());
        }

        try {
            AiHomeCommentClientRequest req = buildRequest(memberId);
            String comment = aiHomeCommentClient.request(req).comment();
            try {
                redisTemplate.opsForValue().set(cacheKey, comment, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("[HomeCommentService] Redis 저장 실패: {}", e.getMessage());
            }
            return comment;
        } catch (Exception e) {
            log.warn("[HomeCommentService] AI 코멘트 생성 실패, fallback 반환: {}", e.getMessage());
            return FALLBACK;
        }
    }

    private AiHomeCommentClientRequest buildRequest(Long memberId) {
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Member not found"));

        int streak = memberStatsRepository.findByMemberId(memberId)
                .map(s -> s.getCurrentStreak().count())
                .orElse(0);

        LocalDate today = LocalDate.now();

        double kcalRate = 0.0;
        double remainingKcal = 0.0;
        var goalOpt = dailyGoalRepository.findByMemberIdAndDate(memberId, today);
        if (goalOpt.isPresent()) {
            DailyGoal goal = goalOpt.get();
            double target = goal.getTargetValue();
            double achieved = goal.getAchievedValue();
            kcalRate = target > 0 ? achieved / target : 0.0;
            remainingKcal = Math.max(target - achieved, 0);
        }

        double proteinG = mealItemRepository.sumProteinByMemberIdAndEffectiveDate(memberId, today);
        double carbG = mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(memberId, today);
        double fatG = mealItemRepository.sumFatByMemberIdAndEffectiveDate(memberId, today);

        return new AiHomeCommentClientRequest(
                memberId,
                member.getHealthGoal().name(),
                streak,
                kcalRate,
                remainingKcal,
                proteinG,
                carbG,
                fatG
        );
    }
}
