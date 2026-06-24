# 홈화면 AI 개인화 코멘트 구현 플랜

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `HomeView.vue`의 `coachMessage`를 HealthGoal + Streak + 칼로리 달성률 기반 Claude 생성 한 줄 코멘트로 교체한다.

**Architecture:** Vue가 `GET /home/comment`(Spring)를 호출하면, Spring이 Redis 캐시(TTL 12h)를 확인 후 MISS 시 FastAPI `POST /ai/home/comment`를 호출해 Claude 코멘트를 받아 캐시하고 반환한다.

**Tech Stack:** FastAPI(Python 3.11), Spring Boot 3.5(Java 21), Redis 7, Vue 3, Pinia

## Global Constraints

- Spring 패키지: `com.ssafy.manager.{domain}.{layer}`
- DTO 접미사: presentation DTO → `*Request`/`*Response`, infrastructure client DTO → `*ClientRequest`/`*ClientResponse`
- Controller 응답: 반드시 `ResponseEntity<T>` 사용
- Application 서비스는 presentation DTO 반환 금지
- Claude 호출은 `claude_service.py`의 `call_claude()` 경유
- FastAPI `ENV=dev`일 때 mock 응답 반환 (Claude 미호출)
- 도메인 간 참조는 `Long` ID만 허용 (`@ManyToOne` 금지)

---

## 파일 목록

| 파일 | 역할 |
|---|---|
| `ai/app/schemas/home.py` | FastAPI 요청·응답 Pydantic 모델 |
| `ai/app/services/home_comment_service.py` | Claude 프롬프트 생성 + 코멘트 반환 |
| `ai/app/routers/ai_home.py` | `POST /ai/home/comment` 라우터 |
| `ai/app/main.py` | ai_home 라우터 등록 |
| `ai/tests/test_home_comment.py` | FastAPI 엔드포인트 테스트 |
| `backend/build.gradle` | Redis 의존성 추가 |
| `backend/src/main/resources/application.properties` | Redis 연결 설정 추가 |
| `backend/.../global/config/RedisConfig.java` | StringRedisTemplate 빈 설정 |
| `backend/.../global/config/RestClientConfig.java` | aiHomeCommentRestClient 빈 추가 |
| `backend/.../home/infrastructure/client/AiHomeCommentClientRequest.java` | FastAPI 송신 record |
| `backend/.../home/infrastructure/client/AiHomeCommentClientResponse.java` | FastAPI 수신 record |
| `backend/.../home/infrastructure/client/AiHomeCommentClient.java` | RestClient 래퍼 |
| `backend/.../home/application/HomeCommentService.java` | Redis 캐싱 + FastAPI 호출 + DB 조회 |
| `backend/src/test/.../home/application/HomeCommentServiceTest.java` | 서비스 단위 테스트 |
| `backend/.../home/presentation/dto/HomeCommentResponse.java` | HTTP 응답 DTO |
| `backend/.../home/presentation/HomeController.java` | `GET /home/comment` |
| `backend/src/test/.../home/presentation/HomeControllerTest.java` | 컨트롤러 테스트 |
| `frontend/src/api/dashboard.js` | `getHomeComment()` 함수 추가 |
| `frontend/src/views/HomeView.vue` | AI 코멘트 연동 |

---

### Task 1: FastAPI — 스키마 + 서비스 + 라우터 + 테스트

**Files:**
- Create: `ai/app/schemas/home.py`
- Create: `ai/app/services/home_comment_service.py`
- Create: `ai/app/routers/ai_home.py`
- Modify: `ai/app/main.py`
- Create: `ai/tests/test_home_comment.py`

**Interfaces:**
- Produces: `POST /ai/home/comment` → `{"comment": "..."}` (Spring Task 3이 소비)

- [ ] **Step 1: 스키마 파일 작성**

`ai/app/schemas/home.py`:
```python
from pydantic import BaseModel
from typing import Literal


class HomeCommentRequest(BaseModel):
    member_id: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    current_streak: int
    kcal_rate: float        # 0.0 ~ 1.0+ (달성률)
    remaining_kcal: float
    protein_g: float
    carb_g: float
    fat_g: float


class HomeCommentResponse(BaseModel):
    comment: str
```

- [ ] **Step 2: 서비스 파일 작성**

`ai/app/services/home_comment_service.py`:
```python
from app.schemas.home import HomeCommentRequest
from app.services.claude_service import call_claude
from app.config import settings

_MOCK_COMMENTS = {
    "DIET": "다이어트 목표를 향해 오늘도 꾸준히 나아가고 있어요. 화이팅!",
    "MUSCLE": "근육 증가 목표, 오늘도 착실히 쌓아가고 있어요!",
    "HEALTH": "건강한 습관이 쌓이고 있어요. 오늘도 좋은 하루 보내세요!",
    "DISEASE": "건강 관리를 꾸준히 이어가고 있어요. 오늘도 잘 하고 있어요!",
}

_GOAL_LABELS = {
    "DIET": "다이어트",
    "MUSCLE": "근육 증가",
    "HEALTH": "건강 유지",
    "DISEASE": "질환 관리",
}


async def generate_home_comment(req: HomeCommentRequest) -> str:
    if settings.env == "dev":
        return _MOCK_COMMENTS.get(req.health_goal, "오늘도 건강한 하루 보내세요!")

    goal_label = _GOAL_LABELS.get(req.health_goal, req.health_goal)
    kcal_percent = round(req.kcal_rate * 100)
    streak_msg = f"Streak {req.current_streak}일 연속 달성 중" if req.current_streak > 0 else "오늘부터 Streak 시작"

    prompt = (
        f"당신은 건강 코치입니다. 아래 사용자 현황을 바탕으로 한 줄(50자 이내) 동기부여 메시지를 작성하세요.\n\n"
        f"[사용자 현황]\n"
        f"- 건강 목표: {goal_label}\n"
        f"- 오늘 칼로리 달성률: {kcal_percent}%\n"
        f"- {streak_msg}\n\n"
        f"[규칙]\n"
        f"- 목표({goal_label})를 중심으로 작성\n"
        f"- 특정 음식이나 영양소 수치 언급 금지\n"
        f"- 구어체, 긍정적 톤, 한국어, 50자 이내\n"
        f"- 한 문장만 출력"
    )

    try:
        return await call_claude(prompt, max_tokens=100)
    except Exception:
        return _MOCK_COMMENTS.get(req.health_goal, "오늘도 건강한 하루 보내세요!")
```

- [ ] **Step 3: 라우터 파일 작성**

`ai/app/routers/ai_home.py`:
```python
from fastapi import APIRouter
from app.schemas.home import HomeCommentRequest, HomeCommentResponse
from app.services.home_comment_service import generate_home_comment

router = APIRouter(prefix="/ai/home", tags=["AI Home"])


@router.post("/comment", response_model=HomeCommentResponse)
async def home_comment(req: HomeCommentRequest):
    comment = await generate_home_comment(req)
    return HomeCommentResponse(comment=comment)
```

- [ ] **Step 4: main.py에 라우터 등록**

`ai/app/main.py` 수정 — import와 `include_router` 한 줄씩 추가:
```python
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report, ai_coach, ai_chat, ai_coaching, ai_home
# ... (기존 코드 유지)
app.include_router(ai_home.router)
```

- [ ] **Step 5: 테스트 작성**

`ai/tests/test_home_comment.py`:
```python
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

BASE_REQUEST = {
    "member_id": 1,
    "health_goal": "MUSCLE",
    "current_streak": 5,
    "kcal_rate": 0.62,
    "remaining_kcal": 680.0,
    "protein_g": 45.0,
    "carb_g": 120.0,
    "fat_g": 28.0,
}


def test_홈_코멘트_200_반환():
    response = client.post("/ai/home/comment", json=BASE_REQUEST)
    assert response.status_code == 200


def test_응답에_comment_필드_존재():
    response = client.post("/ai/home/comment", json=BASE_REQUEST)
    data = response.json()
    assert "comment" in data
    assert isinstance(data["comment"], str)
    assert len(data["comment"]) > 0


def test_DIET_목표_mock_응답():
    req = {**BASE_REQUEST, "health_goal": "DIET", "current_streak": 0}
    response = client.post("/ai/home/comment", json=req)
    assert response.status_code == 200
    assert len(response.json()["comment"]) > 0


def test_DISEASE_목표_mock_응답():
    req = {**BASE_REQUEST, "health_goal": "DISEASE"}
    response = client.post("/ai/home/comment", json=req)
    assert response.status_code == 200


def test_streak_0일_때도_정상_반환():
    req = {**BASE_REQUEST, "current_streak": 0, "kcal_rate": 0.0}
    response = client.post("/ai/home/comment", json=req)
    assert response.status_code == 200
```

- [ ] **Step 6: 테스트 실행 (모두 통과 확인)**

```bash
cd ai
pytest tests/test_home_comment.py -v
```

Expected:
```
test_홈_코멘트_200_반환 PASSED
test_응답에_comment_필드_존재 PASSED
test_DIET_목표_mock_응답 PASSED
test_DISEASE_목표_mock_응답 PASSED
test_streak_0일_때도_정상_반환 PASSED
5 passed
```

- [ ] **Step 7: 커밋**

```bash
git add ai/app/schemas/home.py ai/app/services/home_comment_service.py ai/app/routers/ai_home.py ai/app/main.py ai/tests/test_home_comment.py
git commit -m "feat(ai-home): POST /ai/home/comment Claude 코멘트 생성 엔드포인트"
```

---

### Task 2: Spring — Redis 설정

**Files:**
- Modify: `backend/build.gradle`
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/java/com/ssafy/manager/global/config/RedisConfig.java`

**Interfaces:**
- Produces: `StringRedisTemplate` 빈 (Task 4의 `HomeCommentService`가 소비)

- [ ] **Step 1: build.gradle에 Redis 의존성 추가**

`backend/build.gradle`의 `dependencies` 블록에 한 줄 추가:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

- [ ] **Step 2: application.properties에 Redis 설정 추가**

`backend/src/main/resources/application.properties` 끝에 추가:
```properties
# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
```

- [ ] **Step 3: RedisConfig.java 작성**

`backend/src/main/java/com/ssafy/manager/global/config/RedisConfig.java`:
```java
package com.ssafy.manager.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd backend
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add backend/build.gradle backend/src/main/resources/application.properties backend/src/main/java/com/ssafy/manager/global/config/RedisConfig.java
git commit -m "feat(redis): Spring Redis 의존성 및 StringRedisTemplate 빈 설정"
```

---

### Task 3: Spring — Infrastructure 레이어

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java`
- Create: `backend/src/main/java/com/ssafy/manager/home/infrastructure/client/AiHomeCommentClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/home/infrastructure/client/AiHomeCommentClientResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/home/infrastructure/client/AiHomeCommentClient.java`

**Interfaces:**
- Consumes: `ai.fastapi.url` 환경변수, `POST /ai/home/comment`
- Produces: `AiHomeCommentClient.request(AiHomeCommentClientRequest) → AiHomeCommentClientResponse` (Task 4 소비)

- [ ] **Step 1: AiHomeCommentClientRequest 작성**

`backend/src/main/java/com/ssafy/manager/home/infrastructure/client/AiHomeCommentClientRequest.java`:
```java
package com.ssafy.manager.home.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiHomeCommentClientRequest(
        @JsonProperty("member_id") Long memberId,
        @JsonProperty("health_goal") String healthGoal,
        @JsonProperty("current_streak") int currentStreak,
        @JsonProperty("kcal_rate") double kcalRate,
        @JsonProperty("remaining_kcal") double remainingKcal,
        @JsonProperty("protein_g") double proteinG,
        @JsonProperty("carb_g") double carbG,
        @JsonProperty("fat_g") double fatG
) {}
```

- [ ] **Step 2: AiHomeCommentClientResponse 작성**

`backend/src/main/java/com/ssafy/manager/home/infrastructure/client/AiHomeCommentClientResponse.java`:
```java
package com.ssafy.manager.home.infrastructure.client;

public record AiHomeCommentClientResponse(String comment) {}
```

- [ ] **Step 3: AiHomeCommentClient 작성**

`backend/src/main/java/com/ssafy/manager/home/infrastructure/client/AiHomeCommentClient.java`:
```java
package com.ssafy.manager.home.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiHomeCommentClient {

    private final RestClient aiHomeCommentRestClient;

    public AiHomeCommentClientResponse request(AiHomeCommentClientRequest req) {
        return aiHomeCommentRestClient.post()
                .uri("/ai/home/comment")
                .body(req)
                .retrieve()
                .body(AiHomeCommentClientResponse.class);
    }
}
```

- [ ] **Step 4: RestClientConfig에 빈 추가**

`RestClientConfig.java`의 기존 빈 목록 끝(foodApiRestClient 앞)에 추가:
```java
@Bean
RestClient aiHomeCommentRestClient(
        @Value("${ai.fastapi.url}") String baseUrl) {
    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(fastapiRequestFactory())
            .defaultHeader("Content-Type", "application/json")
            .build();
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd backend
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/home/ backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java
git commit -m "feat(home): AiHomeCommentClient + RestClient 빈 추가"
```

---

### Task 4: Spring — Application 레이어 (HomeCommentService)

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/home/application/HomeCommentService.java`
- Create: `backend/src/test/java/com/ssafy/manager/home/application/HomeCommentServiceTest.java`

**Interfaces:**
- Consumes: `AiHomeCommentClient.request()`, `StringRedisTemplate`, `MemberRepository`, `MemberStatsRepository`, `DailyGoalRepository`, `MealItemRepository`
- Produces: `HomeCommentService.getComment(Long memberId) → String` (Task 5의 Controller 소비)

- [ ] **Step 1: 테스트 파일 작성**

`backend/src/test/java/com/ssafy/manager/home/application/HomeCommentServiceTest.java`:
```java
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
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
cd backend
./gradlew test --tests "com.ssafy.manager.home.application.HomeCommentServiceTest"
```

Expected: FAIL (HomeCommentService 미존재)

- [ ] **Step 3: HomeCommentService 구현**

`backend/src/main/java/com/ssafy/manager/home/application/HomeCommentService.java`:
```java
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
```

- [ ] **Step 4: 테스트 재실행 → 통과 확인**

```bash
cd backend
./gradlew test --tests "com.ssafy.manager.home.application.HomeCommentServiceTest"
```

Expected:
```
Redis_HIT이면_FastAPI를_호출하지_않는다 PASSED
Redis_MISS이면_FastAPI를_호출하고_결과를_캐시에_저장한다 PASSED
FastAPI_호출_실패시_fallback을_반환한다 PASSED
3 tests successful
```

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/home/application/ backend/src/test/java/com/ssafy/manager/home/application/
git commit -m "feat(home): HomeCommentService Redis 캐싱 + FastAPI 호출 구현 (TDD)"
```

---

### Task 5: Spring — Presentation 레이어 (HomeController)

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/home/presentation/dto/HomeCommentResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/home/presentation/HomeController.java`
- Create: `backend/src/test/java/com/ssafy/manager/home/presentation/HomeControllerTest.java`

**Interfaces:**
- Consumes: `HomeCommentService.getComment(Long memberId) → String`
- Produces: `GET /home/comment → ResponseEntity<HomeCommentResponse>` (Vue Task 6 소비)

- [ ] **Step 1: 테스트 파일 작성**

`backend/src/test/java/com/ssafy/manager/home/presentation/HomeControllerTest.java`:
```java
package com.ssafy.manager.home.presentation;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.home.application.HomeCommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class HomeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean HomeCommentService homeCommentService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 인증된_사용자는_코멘트를_조회할_수_있다() throws Exception {
        given(homeCommentService.getComment(MEMBER_ID))
                .willReturn("근육 증가 목표, 오늘도 잘 하고 있어요!");

        mockMvc.perform(get("/home/comment").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("근육 증가 목표, 오늘도 잘 하고 있어요!"));
    }

    @Test
    void 인증_없이_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/home/comment"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
cd backend
./gradlew test --tests "com.ssafy.manager.home.presentation.HomeControllerTest"
```

Expected: FAIL (HomeController 미존재)

- [ ] **Step 3: HomeCommentResponse 작성**

`backend/src/main/java/com/ssafy/manager/home/presentation/dto/HomeCommentResponse.java`:
```java
package com.ssafy.manager.home.presentation.dto;

public record HomeCommentResponse(String comment) {}
```

- [ ] **Step 4: HomeController 작성**

`backend/src/main/java/com/ssafy/manager/home/presentation/HomeController.java`:
```java
package com.ssafy.manager.home.presentation;

import com.ssafy.manager.home.application.HomeCommentService;
import com.ssafy.manager.home.presentation.dto.HomeCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeCommentService homeCommentService;

    @GetMapping("/home/comment")
    public ResponseEntity<HomeCommentResponse> getComment(
            @AuthenticationPrincipal Long memberId) {
        String comment = homeCommentService.getComment(memberId);
        return ResponseEntity.ok(new HomeCommentResponse(comment));
    }
}
```

- [ ] **Step 5: 테스트 재실행 → 통과 확인**

```bash
cd backend
./gradlew test --tests "com.ssafy.manager.home.presentation.HomeControllerTest"
```

Expected:
```
인증된_사용자는_코멘트를_조회할_수_있다 PASSED
인증_없이_접근하면_401을_반환한다 PASSED
2 tests successful
```

- [ ] **Step 6: 전체 테스트 확인**

```bash
cd backend
./gradlew test
```

Expected: `BUILD SUCCESSFUL` (기존 테스트 포함 전체 통과)

- [ ] **Step 7: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/home/presentation/ backend/src/test/java/com/ssafy/manager/home/presentation/
git commit -m "feat(home): GET /home/comment HomeController + HomeCommentResponse (TDD)"
```

---

### Task 6: Vue — API 함수 + HomeView 업데이트

**Files:**
- Modify: `frontend/src/api/dashboard.js`
- Modify: `frontend/src/views/HomeView.vue`

**Interfaces:**
- Consumes: `GET /home/comment → { comment: string }`
- Produces: `coachMessage` 변수가 AI 코멘트 표시

- [ ] **Step 1: dashboard.js에 getHomeComment 추가**

`frontend/src/api/dashboard.js` 파일 끝에 추가:
```js
export function getHomeComment() {
  return apiClient.get('/home/comment')
}
```

- [ ] **Step 2: HomeView.vue — import 추가**

`HomeView.vue`의 import 블록 수정:
```js
import {
  getCalorieBalance,
  getDailySummary,
  getHomeComment,         // 추가
  getLastMealRecommendation,
} from '@/api/dashboard'
```

- [ ] **Step 3: HomeView.vue — aiComment ref 추가**

`HomeView.vue`의 기존 ref 선언부에 한 줄 추가:
```js
const aiComment = ref(null)
```

- [ ] **Step 4: HomeView.vue — coachMessage computed 수정**

기존:
```js
const coachMessage = computed(() => {
  if (state.balance?.lastMealRecommendTrigger) return '마지막 끼니 추천이 준비됐어요!'
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 기록해볼까요?`
})
```

변경 후:
```js
const coachMessage = computed(() => {
  if (aiComment.value) return aiComment.value
  if (state.balance?.lastMealRecommendTrigger) return '마지막 끼니 추천이 준비됐어요!'
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 기록해볼까요?`
})
```

- [ ] **Step 5: HomeView.vue — onMounted에 AI 코멘트 fetch 추가**

기존 `onMounted` 블록:
```js
onMounted(async () => {
  const [summary, balance] = await Promise.allSettled([
    getDailySummary(today),
    getCalorieBalance(today),
  ])
  // ...기존 코드...
})
```

변경 후:
```js
onMounted(async () => {
  const [summary, balance] = await Promise.allSettled([
    getDailySummary(today),
    getCalorieBalance(today),
  ])

  state.summary = summary.status === 'fulfilled' ? summary.value : null
  state.balance = balance.status === 'fulfilled' ? balance.value : null
  state.loading = false

  setTimeout(() => {
    strokeDashoffset.value = circumference - (calorieProgress.value / 100) * circumference
  }, 300)

  if (state.balance?.lastMealRecommendTrigger) {
    loadRecommendation()
  }

  // AI 코멘트 비동기 로드 (실패 시 정적 fallback 유지)
  try {
    const res = await getHomeComment()
    aiComment.value = res.comment
  } catch {
    // aiComment null 유지 → coachMessage computed가 정적 fallback 반환
  }
})
```

- [ ] **Step 6: 동작 확인**

Spring + FastAPI + Redis 로컬 실행 후:
1. 홈 화면 접속
2. 냠냠 캐릭터 옆 코멘트가 AI 생성 메시지로 표시되는지 확인
3. 개발자 도구 Network 탭에서 `GET /api/home/comment` 200 응답 확인
4. 페이지 새로고침 시 Redis HIT (동일 코멘트 빠르게 반환) 확인

Redis 미실행 환경에서는 Spring 로그에 `Redis 조회 실패` warn이 출력되며 FastAPI를 직접 호출하여 정상 동작.

- [ ] **Step 7: 커밋**

```bash
git add frontend/src/api/dashboard.js frontend/src/views/HomeView.vue
git commit -m "feat(home): 홈화면 AI 개인화 코멘트 Vue 연동"
```
