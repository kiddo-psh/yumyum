package com.ssafy.manager.home.application;

import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.domain.Streak;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.home.infrastructure.client.AiHomeCommentClient;
import com.ssafy.manager.home.infrastructure.client.AiHomeCommentClientRequest;
import com.ssafy.manager.home.infrastructure.client.AiHomeCommentClientResponse;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeCommentServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock MemberStatsRepository memberStatsRepository;
    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock AiHomeCommentClient aiHomeCommentClient;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks HomeCommentService homeCommentService;

    private static final Long MEMBER_ID = 1L;
    private static final String CACHE_KEY = "home_comment:1";

    @Test
    void Redis_HIT이면_FastAPI를_호출하지_않는다() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(CACHE_KEY)).willReturn("캐시된 코멘트입니다.");

        String result = homeCommentService.getComment(MEMBER_ID);

        assertThat(result).isEqualTo("캐시된 코멘트입니다.");
        verify(aiHomeCommentClient, never()).request(any());
    }

    @Test
    void Redis_MISS이면_FastAPI를_호출하고_결과를_캐시에_저장한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(CACHE_KEY)).willReturn(null);

        Member member = mock(Member.class);
        given(member.getHealthGoal()).willReturn(HealthGoal.MUSCLE);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        MemberStats stats = new MemberStats(Streak.of(5), Streak.of(5), LocalDate.now().minusDays(1));
        given(memberStatsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(stats));

        DailyGoal goal = DailyGoal.of(MEMBER_ID, LocalDate.now(), 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(eq(MEMBER_ID), any())).willReturn(Optional.of(goal));
        given(mealItemRepository.sumProteinByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(45.0);
        given(mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(120.0);
        given(mealItemRepository.sumFatByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(28.0);

        given(aiHomeCommentClient.request(any(AiHomeCommentClientRequest.class)))
                .willReturn(new AiHomeCommentClientResponse("근육 증가 목표, 오늘도 잘 하고 있어요!"));

        String result = homeCommentService.getComment(MEMBER_ID);

        assertThat(result).isEqualTo("근육 증가 목표, 오늘도 잘 하고 있어요!");
        verify(valueOps).set(eq(CACHE_KEY), eq("근육 증가 목표, 오늘도 잘 하고 있어요!"), eq(12L), any());
    }

    @Test
    void FastAPI_호출_실패시_fallback을_반환한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(CACHE_KEY)).willReturn(null);

        Member member = mock(Member.class);
        given(member.getHealthGoal()).willReturn(HealthGoal.DIET);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberStatsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        given(dailyGoalRepository.findByMemberIdAndDate(eq(MEMBER_ID), any())).willReturn(Optional.empty());
        given(mealItemRepository.sumProteinByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(0.0);
        given(mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(0.0);
        given(mealItemRepository.sumFatByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(0.0);

        given(aiHomeCommentClient.request(any())).willThrow(new RuntimeException("FastAPI timeout"));

        String result = homeCommentService.getComment(MEMBER_ID);

        assertThat(result).isEqualTo("오늘도 건강한 하루 보내세요!");
        verify(valueOps, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    void Redis_읽기_오류시_FastAPI를_직접_호출한다() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(CACHE_KEY)).willThrow(new org.springframework.data.redis.RedisConnectionFailureException("연결 실패"));

        Member member = mock(Member.class);
        given(member.getHealthGoal()).willReturn(HealthGoal.MUSCLE);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        MemberStats stats = new MemberStats(Streak.of(3), Streak.of(3), LocalDate.now().minusDays(1));
        given(memberStatsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(stats));

        DailyGoal goal = DailyGoal.of(MEMBER_ID, LocalDate.now(), 2000.0);
        given(dailyGoalRepository.findByMemberIdAndDate(eq(MEMBER_ID), any())).willReturn(Optional.of(goal));
        given(mealItemRepository.sumProteinByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(50.0);
        given(mealItemRepository.sumCarbsByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(130.0);
        given(mealItemRepository.sumFatByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(30.0);

        given(aiHomeCommentClient.request(any(AiHomeCommentClientRequest.class)))
                .willReturn(new AiHomeCommentClientResponse("직접 호출 코멘트"));

        String result = homeCommentService.getComment(MEMBER_ID);

        assertThat(result).isEqualTo("직접 호출 코멘트");
        verify(aiHomeCommentClient).request(any());
    }
}
