# F705 스마트 알림 타이밍 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자가 앱을 열면 현재 상태(Streak 위기·식사 미기록·운동 미기록·주간 리포트 생성)를 실시간 평가해 Claude가 개인화 메시지와 권장 시각을 생성하는 인앱 알림 시스템을 구축한다.

**Architecture:** `GET /notifications` 호출 시 Spring이 규칙 기반으로 트리거 조건을 평가하고 FastAPI `POST /ai/notification/timing`을 통해 Claude가 메시지+시각을 생성, Redis(1h TTL)에 캐시한다. 식단/운동 저장 시 캐시를 무효화해 다음 조회에서 최신 상태를 반영한다.

**Tech Stack:** FastAPI(Python), Spring Boot 3.3/Java 21, Vue 3, Redis(StringRedisTemplate + Jackson), RestClient

## Global Constraints

- 패키지: `com.ssafy.manager.notification.{layer}`
- DTO 접미사: presentation → `Response`, application → `Result`, infrastructure/client → `ClientRequest`/`ClientResponse`
- 컨트롤러 응답: `ResponseEntity<T>` 필수
- snake_case JSON: `@JsonProperty` 사용 (글로벌 Jackson 설정 금지)
- `@Slf4j` + Lombok
- AI 호출 실패: `catch (Exception e)` → `log.warn` → 빈 목록 반환 (예외 전파 없음)
- 크로스 도메인 참조: plain `Long` ID만, `@ManyToOne`/`@OneToMany` 금지
- FastAPI mock 모드: `settings.env == "dev"`일 때 Claude 미호출, 고정 응답 반환
- Redis 캐시 키: `notifications:{memberId}`, TTL: 1h
- `Streak.count()` 메서드 사용 (getCount() 아님)

---

## 파일 구조

```
FastAPI 신규:
  ai/app/schemas/notification.py
  ai/app/services/notification_service.py
  ai/app/routers/ai_notification.py
  ai/tests/test_notification.py
FastAPI 수정:
  ai/app/main.py                         (router 등록)
  ai/app/services/claude_service.py      (mock 분기 추가)

Spring 신규:
  backend/.../notification/infrastructure/client/AiNotificationClientRequest.java
  backend/.../notification/infrastructure/client/AiNotificationClientResponse.java
  backend/.../notification/infrastructure/client/AiNotificationClient.java
  backend/.../notification/application/NotificationResult.java
  backend/.../notification/application/NotificationService.java
  backend/.../notification/presentation/dto/NotificationResponse.java
  backend/.../notification/presentation/NotificationController.java
  backend/src/test/.../notification/application/NotificationServiceTest.java
  backend/src/test/.../notification/presentation/NotificationControllerTest.java
Spring 수정:
  backend/.../nutrition/domain/Meal.java                        (recordedAt 추가)
  backend/.../nutrition/application/MealService.java            (recordedAt 저장 + Redis 무효화)
  backend/.../program/domain/WeeklyReport.java                  (filledAt 추가)
  backend/.../program/infrastructure/persistence/WeeklyReportRepository.java  (쿼리 추가)
  backend/.../routine/application/RoutineSessionService.java    (Redis 무효화)
  backend/.../global/config/RestClientConfig.java               (aiNotificationRestClient 빈)

Vue 신규:
  frontend/src/api/notification.js
  frontend/src/composables/useNotifications.js
  frontend/src/components/NotificationBell.vue
Vue 수정:
  frontend/src/App.vue                   (NotificationBell 마운트 + onMounted fetch)
```

---

### Task 1: FastAPI — 스키마 + 서비스 + 엔드포인트

**Files:**
- Create: `ai/app/schemas/notification.py`
- Create: `ai/app/services/notification_service.py`
- Create: `ai/app/routers/ai_notification.py`
- Modify: `ai/app/main.py`
- Modify: `ai/app/services/claude_service.py`
- Create: `ai/tests/test_notification.py`

**Interfaces:**
- Produces: `POST /ai/notification/timing` → `NotificationTimingResponse`

- [ ] **Step 1: 스키마 파일 작성**

`ai/app/schemas/notification.py`:
```python
from pydantic import BaseModel
from typing import List, Literal


class NotificationContext(BaseModel):
    today_kcal_rate: float   # 0.0 ~ 1.0+
    meal_count: int
    this_week_sessions: int


class NotificationTimingRequest(BaseModel):
    member_id: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    current_streak: int
    triggered_types: List[Literal["STREAK_RISK", "MEAL_REMINDER", "EXERCISE_REMINDER", "WEEKLY_REPORT_READY"]]
    context: NotificationContext


class NotificationItem(BaseModel):
    type: str
    message: str
    recommended_time: str   # "HH:MM" 또는 "now"
    priority: int


class NotificationTimingResponse(BaseModel):
    notifications: List[NotificationItem]
```

- [ ] **Step 2: notification_service.py 작성**

`ai/app/services/notification_service.py`:
```python
import json
from app.schemas.notification import (
    NotificationTimingRequest, NotificationTimingResponse, NotificationItem
)
from app.services.claude_service import call_claude

# HealthGoal별 기본 시각 (Claude가 이를 기반으로 조정)
_DEFAULT_TIMES = {
    "STREAK_RISK":         {"DIET": "19:30", "MUSCLE": "21:00", "HEALTH": "20:00", "DISEASE": "20:00"},
    "MEAL_REMINDER":       {"DIET": "12:00", "MUSCLE": "11:30", "HEALTH": "12:00", "DISEASE": "12:00"},
    "EXERCISE_REMINDER":   {"DIET": "18:30", "MUSCLE": "17:00", "HEALTH": "18:00", "DISEASE": "18:30"},
    "WEEKLY_REPORT_READY": {"DIET": "now",   "MUSCLE": "now",   "HEALTH": "now",   "DISEASE": "now"},
}

_TYPE_LABELS = {
    "STREAK_RISK":         "Streak 위기",
    "MEAL_REMINDER":       "식사 리마인더",
    "EXERCISE_REMINDER":   "운동 리마인더",
    "WEEKLY_REPORT_READY": "주간 리포트 준비",
}


async def generate_notifications(req: NotificationTimingRequest) -> NotificationTimingResponse:
    if not req.triggered_types:
        return NotificationTimingResponse(notifications=[])

    items = []
    for priority, ntype in enumerate(req.triggered_types, start=1):
        default_time = _DEFAULT_TIMES.get(ntype, {}).get(req.health_goal, "20:00")
        prompt = _build_prompt(req, ntype, default_time)
        raw = await call_claude(prompt, max_tokens=200)
        message, recommended_time = _parse_response(raw, default_time)
        items.append(NotificationItem(
            type=ntype,
            message=message,
            recommended_time=recommended_time,
            priority=priority,
        ))

    return NotificationTimingResponse(notifications=items)


def _build_prompt(req: NotificationTimingRequest, ntype: str, default_time: str) -> str:
    label = _TYPE_LABELS.get(ntype, ntype)
    ctx = req.context
    return f"""사용자 알림 메시지를 생성하세요.

알림 유형: {label}
건강 목표: {req.health_goal}
현재 Streak: {req.current_streak}일
오늘 칼로리 달성률: {ctx.today_kcal_rate * 100:.0f}%
오늘 식사 기록 수: {ctx.meal_count}
이번 주 운동 세션: {ctx.this_week_sessions}회
기본 권장 발송 시각: {default_time}

다음 JSON 형식으로만 응답하세요:
{{"message": "한국어 알림 메시지 (1~2문장)", "recommended_time": "HH:MM 또는 now"}}"""


def _parse_response(raw: str, default_time: str):
    try:
        cleaned = raw.strip()
        if "```" in cleaned:
            parts = cleaned.split("```")
            cleaned = parts[1] if len(parts) > 1 else cleaned
            if cleaned.lower().startswith("json"):
                cleaned = cleaned[4:]
        data = json.loads(cleaned)
        return data["message"], data.get("recommended_time", default_time)
    except Exception:
        return "지금 앱을 확인해보세요!", default_time
```

- [ ] **Step 3: claude_service.py에 mock 분기 추가**

`ai/app/services/claude_service.py`의 `_mock_response` 함수 맨 위에 아래 분기 추가 (기존 코드 사이):
```python
    if "알림 메시지를 생성" in prompt:
        # 첫 번째 줄에서 알림 유형 추출
        if "Streak 위기" in prompt:
            return '{"message": "오늘 Streak이 끊길 위험이에요! 저녁 식단을 기록해보세요.", "recommended_time": "20:00"}'
        elif "식사 리마인더" in prompt:
            return '{"message": "오늘 아직 목표 칼로리의 절반도 채우지 않으셨네요. 점심을 기록해보세요!", "recommended_time": "12:00"}'
        elif "운동 리마인더" in prompt:
            return '{"message": "이번 주 아직 운동을 시작하지 않으셨네요. 오늘 루틴을 시작해볼까요?", "recommended_time": "18:00"}'
        elif "주간 리포트" in prompt:
            return '{"message": "이번 주 AI 코칭 리포트가 준비됐어요! 확인해보세요.", "recommended_time": "now"}'
        return '{"message": "오늘 하루도 건강 목표를 향해 나아가고 있어요!", "recommended_time": "20:00"}'
```

- [ ] **Step 4: 라우터 작성**

`ai/app/routers/ai_notification.py`:
```python
from fastapi import APIRouter
from app.schemas.notification import NotificationTimingRequest, NotificationTimingResponse
from app.services.notification_service import generate_notifications

router = APIRouter(tags=["AI Notification"])


@router.post("/ai/notification/timing", response_model=NotificationTimingResponse)
async def notification_timing(req: NotificationTimingRequest):
    """
    F705 - 스마트 알림 타이밍
    triggered_types별로 Claude가 개인화 메시지 + 권장 발송 시각을 생성한다.
    triggered_types가 빈 배열이면 즉시 빈 목록 반환 (Claude 미호출).
    """
    return await generate_notifications(req)
```

- [ ] **Step 5: main.py에 라우터 등록**

`ai/app/main.py` 3번째 줄 import에 `ai_notification` 추가:
```python
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report, ai_coach, ai_chat, ai_coaching, ai_notification
```

`app.include_router(ai_coaching.router)` 다음 줄에 추가:
```python
app.include_router(ai_notification.router)
```

- [ ] **Step 6: 테스트 작성**

`ai/tests/test_notification.py`:
```python
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app


@pytest.mark.asyncio
async def test_streak_risk_알림이_생성된다():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/ai/notification/timing", json={
            "member_id": 1,
            "health_goal": "MUSCLE",
            "current_streak": 5,
            "triggered_types": ["STREAK_RISK"],
            "context": {"today_kcal_rate": 0.3, "meal_count": 1, "this_week_sessions": 0},
        })
    assert resp.status_code == 200
    data = resp.json()
    assert len(data["notifications"]) == 1
    n = data["notifications"][0]
    assert n["type"] == "STREAK_RISK"
    assert len(n["message"]) > 0
    assert ":" in n["recommended_time"]  # "HH:MM" 형식


@pytest.mark.asyncio
async def test_triggered_types가_비어있으면_빈_목록을_반환한다():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/ai/notification/timing", json={
            "member_id": 1,
            "health_goal": "DIET",
            "current_streak": 0,
            "triggered_types": [],
            "context": {"today_kcal_rate": 1.0, "meal_count": 3, "this_week_sessions": 2},
        })
    assert resp.status_code == 200
    assert resp.json()["notifications"] == []


@pytest.mark.asyncio
async def test_복수_알림_유형이_모두_생성된다():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/ai/notification/timing", json={
            "member_id": 2,
            "health_goal": "DIET",
            "current_streak": 3,
            "triggered_types": ["MEAL_REMINDER", "EXERCISE_REMINDER"],
            "context": {"today_kcal_rate": 0.2, "meal_count": 0, "this_week_sessions": 0},
        })
    assert resp.status_code == 200
    notifications = resp.json()["notifications"]
    assert len(notifications) == 2
    types = {n["type"] for n in notifications}
    assert types == {"MEAL_REMINDER", "EXERCISE_REMINDER"}


@pytest.mark.asyncio
async def test_weekly_report_ready_알림_recommended_time이_now이다():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/ai/notification/timing", json={
            "member_id": 1,
            "health_goal": "HEALTH",
            "current_streak": 7,
            "triggered_types": ["WEEKLY_REPORT_READY"],
            "context": {"today_kcal_rate": 0.9, "meal_count": 2, "this_week_sessions": 3},
        })
    assert resp.status_code == 200
    n = resp.json()["notifications"][0]
    assert n["type"] == "WEEKLY_REPORT_READY"
    assert n["recommended_time"] == "now"
```

- [ ] **Step 7: 테스트 실행**

```bash
cd ai
pytest tests/test_notification.py -v
```

Expected: 4 passed

- [ ] **Step 8: 커밋**

```bash
git add ai/app/schemas/notification.py ai/app/services/notification_service.py \
        ai/app/routers/ai_notification.py ai/app/main.py \
        ai/app/services/claude_service.py ai/tests/test_notification.py
git commit -m "feat(notification): F705 POST /ai/notification/timing 구현"
```

---

### Task 2: Spring 엔티티 + 리포지토리 변경

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/domain/Meal.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/application/MealService.java`
- Modify: `backend/src/main/java/com/ssafy/manager/program/domain/WeeklyReport.java`
- Modify: `backend/src/main/java/com/ssafy/manager/program/infrastructure/persistence/WeeklyReportRepository.java`
- Test: `backend/src/test/java/com/ssafy/manager/program/domain/WeeklyReportTest.java`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `Meal.recordedAt: LocalDateTime` (getter 자동 생성)
  - `WeeklyReport.filledAt: LocalDateTime` (getter 자동 생성)
  - `WeeklyReportRepository.findFirstByProgramIdAndContentIsNotNullOrderByWeekNumberDesc(Long): Optional<WeeklyReport>`

- [ ] **Step 1: WeeklyReportTest에 실패 테스트 작성**

`backend/src/test/java/com/ssafy/manager/program/domain/WeeklyReportTest.java`에 테스트 추가:
```java
@Test
void fill_호출_시_filledAt이_설정된다() {
    WeeklyReport report = new WeeklyReport(1L, 1);
    assertThat(report.getFilledAt()).isNull();

    report.fill("content", "nutrition", "exercise", "goal", 0.9, 6, -0.5);

    assertThat(report.getFilledAt()).isNotNull();
    assertThat(report.getFilledAt()).isBeforeOrEqualTo(LocalDateTime.now());
}
```

필요한 import: `import java.time.LocalDateTime;`

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.program.domain.WeeklyReportTest" -q
```

Expected: FAIL (filledAt 필드 없음)

- [ ] **Step 3: WeeklyReport.java 수정**

`filledAt` 필드 추가 및 `fill()` 수정:
```java
import java.time.LocalDateTime;

// 기존 필드들 뒤에 추가
private LocalDateTime filledAt;

// fill() 메서드 마지막에 한 줄 추가
public void fill(String content, String nutritionSummary, String exerciseSummary,
                 String goalSummary, double avgCalorieRate, int achievementDays,
                 Double weightTrend) {
    this.content = content;
    this.nutritionSummary = nutritionSummary;
    this.exerciseSummary = exerciseSummary;
    this.goalSummary = goalSummary;
    this.avgCalorieRate = avgCalorieRate;
    this.achievementDays = achievementDays;
    this.weightTrend = weightTrend;
    this.filledAt = LocalDateTime.now();  // 추가
}
```

- [ ] **Step 4: 테스트 실행 (통과 확인)**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.program.domain.WeeklyReportTest" -q
```

Expected: PASS

- [ ] **Step 5: WeeklyReportRepository에 쿼리 추가**

`WeeklyReportRepository.java`에 메서드 추가:
```java
Optional<WeeklyReport> findFirstByProgramIdAndContentIsNotNullOrderByWeekNumberDesc(Long programId);
```

- [ ] **Step 6: Meal.java에 recordedAt 추가**

기존 필드 선언부에 추가:
```java
import java.time.LocalDateTime;

// effectiveDate 필드 다음에 추가
private LocalDateTime recordedAt;
```

기존 생성자를 확장:
```java
// 기존 생성자 교체
public Meal(Long memberId, MealType type, LocalDate date, LocalDate effectiveDate, LocalDateTime recordedAt) {
    this.memberId = memberId;
    this.type = type;
    this.date = date;
    this.effectiveDate = effectiveDate;
    this.recordedAt = recordedAt;
}
```

- [ ] **Step 7: MealService.java에서 Meal 생성자 호출부 수정**

`MealService.record()` 안의 Meal 생성 코드 수정:
```java
// 기존
Meal meal = new Meal(command.memberId(), resolvedType, command.date(), effectiveDate);
// 수정
Meal meal = new Meal(command.memberId(), resolvedType, command.date(), effectiveDate, recordedAt);
```

`MealService.recordFromPhoto()` 안의 Meal 생성 코드 수정:
```java
// 기존
Meal meal = new Meal(command.memberId(), resolvedType, recordedAt.toLocalDate(), effectiveDate);
// 수정
Meal meal = new Meal(command.memberId(), resolvedType, recordedAt.toLocalDate(), effectiveDate, recordedAt);
```

- [ ] **Step 8: 전체 빌드 확인**

```bash
cd backend && ./gradlew build -x test -q
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/nutrition/domain/Meal.java \
        backend/src/main/java/com/ssafy/manager/nutrition/application/MealService.java \
        backend/src/main/java/com/ssafy/manager/program/domain/WeeklyReport.java \
        backend/src/main/java/com/ssafy/manager/program/infrastructure/persistence/WeeklyReportRepository.java \
        backend/src/test/java/com/ssafy/manager/program/domain/WeeklyReportTest.java
git commit -m "feat(notification): Meal.recordedAt + WeeklyReport.filledAt 추가"
```

---

### Task 3: AiNotificationClient + RestClientConfig 빈

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/notification/infrastructure/client/AiNotificationClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/notification/infrastructure/client/AiNotificationClientResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/notification/infrastructure/client/AiNotificationClient.java`
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java`

**Interfaces:**
- Produces: `AiNotificationClient.requestTiming(AiNotificationClientRequest): AiNotificationClientResponse`

- [ ] **Step 1: AiNotificationClientRequest.java 작성**

```java
package com.ssafy.manager.notification.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiNotificationClientRequest(
        @JsonProperty("member_id")       Long memberId,
        @JsonProperty("health_goal")     String healthGoal,
        @JsonProperty("current_streak")  int currentStreak,
        @JsonProperty("triggered_types") List<String> triggeredTypes,
        @JsonProperty("context")         Context context
) {
    public record Context(
            @JsonProperty("today_kcal_rate")      double todayKcalRate,
            @JsonProperty("meal_count")           int mealCount,
            @JsonProperty("this_week_sessions")   int thisWeekSessions
    ) {}
}
```

- [ ] **Step 2: AiNotificationClientResponse.java 작성**

```java
package com.ssafy.manager.notification.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiNotificationClientResponse(
        @JsonProperty("notifications") List<NotificationItem> notifications
) {
    public record NotificationItem(
            @JsonProperty("type")             String type,
            @JsonProperty("message")          String message,
            @JsonProperty("recommended_time") String recommendedTime,
            @JsonProperty("priority")         int priority
    ) {}
}
```

- [ ] **Step 3: AiNotificationClient.java 작성**

```java
package com.ssafy.manager.notification.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiNotificationClient {

    private final RestClient aiNotificationRestClient;

    public AiNotificationClientResponse requestTiming(AiNotificationClientRequest request) {
        return aiNotificationRestClient.post()
                .uri("/ai/notification/timing")
                .body(request)
                .retrieve()
                .body(AiNotificationClientResponse.class);
    }
}
```

- [ ] **Step 4: RestClientConfig.java에 빈 추가**

기존 `aiCoachingRestClient` 빈 다음에 추가:
```java
@Bean
RestClient aiNotificationRestClient(
        @Value("${ai.fastapi.url}") String baseUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3_000);
    factory.setReadTimeout(15_000);
    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("Content-Type", "application/json")
            .build();
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd backend && ./gradlew build -x test -q
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/notification/ \
        backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java
git commit -m "feat(notification): AiNotificationClient + RestClient 빈 추가"
```

---

### Task 4: NotificationService (트리거 평가 + Redis 캐시)

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/notification/application/NotificationResult.java`
- Create: `backend/src/main/java/com/ssafy/manager/notification/application/NotificationService.java`
- Create: `backend/src/test/java/com/ssafy/manager/notification/application/NotificationServiceTest.java`

**Interfaces:**
- Consumes:
  - `AiNotificationClient.requestTiming(AiNotificationClientRequest): AiNotificationClientResponse`
  - `DailyGoalRepository.findByMemberIdAndDate(Long, LocalDate): Optional<DailyGoal>`
  - `MemberStatsRepository.findByMemberId(Long): Optional<MemberStats>`
  - `MealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(Long, LocalDate): double`
  - `MealRepository.countByMemberIdAndEffectiveDate(Long, LocalDate): int`
  - `ProgramRepository.findByMemberIdAndStatus(Long, ProgramStatus): Optional<Program>`
  - `RoutineSessionRepository.countDistinctSessionDatesByMemberIdAndDateBetween(Long, LocalDate, LocalDate): int`
  - `RoutineRepository.findTopByMemberIdOrderByCreatedAtDesc(Long): Optional<Routine>`
  - `WeeklyReportRepository.findFirstByProgramIdAndContentIsNotNullOrderByWeekNumberDesc(Long): Optional<WeeklyReport>`
  - `StringRedisTemplate` (캐시 읽기/쓰기)
- Produces: `NotificationService.getNotifications(Long memberId): List<NotificationResult>`
- Produces: `NotificationService.evictCache(Long memberId): void`

- [ ] **Step 1: NotificationResult.java 작성**

```java
package com.ssafy.manager.notification.application;

import com.ssafy.manager.notification.infrastructure.client.AiNotificationClientResponse;

public record NotificationResult(
        String type,
        String message,
        String recommendedTime,
        int priority
) {
    public static NotificationResult from(AiNotificationClientResponse.NotificationItem item) {
        return new NotificationResult(item.type(), item.message(), item.recommendedTime(), item.priority());
    }
}
```

- [ ] **Step 2: 실패 테스트 작성**

`backend/src/test/java/com/ssafy/manager/notification/application/NotificationServiceTest.java`:
```java
package com.ssafy.manager.notification.application;

import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.domain.Streak;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.member.domain.HealthGoal;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.notification.infrastructure.client.AiNotificationClient;
import com.ssafy.manager.notification.infrastructure.client.AiNotificationClientResponse;
import com.ssafy.manager.notification.infrastructure.client.AiNotificationClientResponse.NotificationItem;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock MemberStatsRepository memberStatsRepository;
    @Mock DailyGoalRepository dailyGoalRepository;
    @Mock MealItemRepository mealItemRepository;
    @Mock MealRepository mealRepository;
    @Mock ProgramRepository programRepository;
    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock RoutineRepository routineRepository;
    @Mock WeeklyReportRepository weeklyReportRepository;
    @Mock AiNotificationClient aiNotificationClient;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ObjectMapper objectMapper;

    @InjectMocks NotificationService notificationService;

    private static final Long MEMBER_ID = 1L;

    @Test
    void Redis_캐시가_있으면_FastAPI를_호출하지_않는다() throws Exception {
        String cached = "[{\"type\":\"STREAK_RISK\",\"message\":\"cached\",\"recommendedTime\":\"20:00\",\"priority\":1}]";
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("notifications:" + MEMBER_ID)).willReturn(cached);
        given(objectMapper.readValue(eq(cached), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .willReturn(List.of(new NotificationResult("STREAK_RISK", "cached", "20:00", 1)));

        List<NotificationResult> result = notificationService.getNotifications(MEMBER_ID);

        assertThat(result).hasSize(1);
        verifyNoInteractions(aiNotificationClient);
    }

    @Test
    void triggered_types가_없으면_FastAPI를_호출하지_않는다() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);

        Member member = mock(Member.class);
        given(member.getHealthGoal()).willReturn(HealthGoal.HEALTH);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberStatsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty()); // streak = 0 → STREAK_RISK 불충족

        // Program 2400kcal + 오늘 섭취 2000kcal → kcalRate = 0.83 → MEAL_REMINDER 불충족 (시각 무관)
        Program program = Program.create(MEMBER_ID, ProgramType.HEALTH, LocalDate.now(), LocalDate.now().plusDays(27),
                2400, 100.0, 200.0, 50.0, null);
        given(programRepository.findByMemberIdAndStatus(eq(MEMBER_ID), eq(ProgramStatus.ACTIVE))).willReturn(Optional.of(program));
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(2000.0);

        given(dailyGoalRepository.findByMemberIdAndDate(eq(MEMBER_ID), any())).willReturn(Optional.empty());
        given(routineRepository.findTopByMemberIdOrderByCreatedAtDesc(MEMBER_ID)).willReturn(Optional.empty()); // hasRoutine = false → EXERCISE_REMINDER 불충족
        given(routineSessionRepository.countDistinctSessionDatesByMemberIdAndDateBetween(eq(MEMBER_ID), any(), any())).willReturn(0);
        given(weeklyReportRepository.findFirstByProgramIdAndContentIsNotNullOrderByWeekNumberDesc(null)).willReturn(Optional.empty()); // WEEKLY_REPORT_READY 불충족
        given(objectMapper.writeValueAsString(any())).willReturn("[]");

        List<NotificationResult> result = notificationService.getNotifications(MEMBER_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(aiNotificationClient);
    }

    @Test
    void AI_호출_실패_시_빈_목록을_반환하고_예외를_전파하지_않는다() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);

        Member member = mock(Member.class);
        given(member.getHealthGoal()).willReturn(HealthGoal.MUSCLE);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        MemberStats stats = mock(MemberStats.class);
        given(stats.getCurrentStreak()).willReturn(Streak.of(5));
        given(memberStatsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(stats));

        // STREAK_RISK 트리거 (DailyGoal 미달성 + streak 5 + 시각 체크는 서비스 내 LocalTime.now() 사용)
        DailyGoal goal = mock(DailyGoal.class);
        given(goal.isAchieved()).willReturn(false);
        given(dailyGoalRepository.findByMemberIdAndDate(eq(MEMBER_ID), any())).willReturn(Optional.of(goal));
        given(programRepository.findByMemberIdAndStatus(eq(MEMBER_ID), eq(ProgramStatus.ACTIVE))).willReturn(Optional.empty());
        given(mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(0.0);
        given(mealRepository.countByMemberIdAndEffectiveDate(eq(MEMBER_ID), any())).willReturn(0);
        given(routineRepository.findTopByMemberIdOrderByCreatedAtDesc(MEMBER_ID)).willReturn(Optional.empty());
        given(aiNotificationClient.requestTiming(any())).willThrow(new RuntimeException("FastAPI 연결 실패"));
        given(objectMapper.writeValueAsString(List.of())).willReturn("[]");

        List<NotificationResult> result = notificationService.getNotifications(MEMBER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void evictCache_호출_시_Redis_키가_삭제된다() {
        given(redisTemplate.delete("notifications:" + MEMBER_ID)).willReturn(true);

        notificationService.evictCache(MEMBER_ID);

        verify(redisTemplate).delete("notifications:" + MEMBER_ID);
    }
}
```

- [ ] **Step 3: 테스트 실행 (실패 확인)**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.notification.application.NotificationServiceTest" -q
```

Expected: FAIL (NotificationService 없음)

- [ ] **Step 4: NotificationService.java 작성**

```java
package com.ssafy.manager.notification.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.growth.domain.MemberStats;
import com.ssafy.manager.growth.infrastructure.persistence.MemberStatsRepository;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.notification.infrastructure.client.AiNotificationClient;
import com.ssafy.manager.notification.infrastructure.client.AiNotificationClientRequest;
import com.ssafy.manager.notification.infrastructure.client.AiNotificationClientResponse;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealItemRepository;
import com.ssafy.manager.nutrition.infrastructure.persistence.MealRepository;
import com.ssafy.manager.program.domain.DailyGoal;
import com.ssafy.manager.program.domain.Program;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.infrastructure.persistence.DailyGoalRepository;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.program.infrastructure.persistence.WeeklyReportRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MemberRepository memberRepository;
    private final MemberStatsRepository memberStatsRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final ProgramRepository programRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final RoutineRepository routineRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final AiNotificationClient aiNotificationClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX = "notifications:";

    @Transactional(readOnly = true)
    public List<NotificationResult> getNotifications(Long memberId) {
        String key = KEY_PREFIX + memberId;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("알림 캐시 읽기 실패 memberId={}: {}", memberId, e.getMessage());
        }

        List<NotificationResult> result = buildNotifications(memberId);
        cacheResult(key, result);
        return result;
    }

    public void evictCache(Long memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }

    private List<NotificationResult> buildNotifications(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Optional<MemberStats> statsOpt = memberStatsRepository.findByMemberId(memberId);
        int currentStreak = statsOpt.map(s -> s.getCurrentStreak().count()).orElse(0);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<String> triggeredTypes = new ArrayList<>();
        double kcalRate = evaluateTriggers(memberId, statsOpt.orElse(null), currentStreak, today, now, triggeredTypes);

        if (triggeredTypes.isEmpty()) {
            return List.of();
        }

        int mealCount = mealRepository.countByMemberIdAndEffectiveDate(memberId, today);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        int thisWeekSessions = routineSessionRepository
                .countDistinctSessionDatesByMemberIdAndDateBetween(memberId, weekStart, today);

        AiNotificationClientRequest req = new AiNotificationClientRequest(
                memberId,
                member.getHealthGoal().name(),
                currentStreak,
                triggeredTypes,
                new AiNotificationClientRequest.Context(kcalRate, mealCount, thisWeekSessions)
        );

        try {
            AiNotificationClientResponse resp = aiNotificationClient.requestTiming(req);
            return resp.notifications().stream()
                       .map(NotificationResult::from)
                       .toList();
        } catch (Exception e) {
            log.warn("알림 생성 FastAPI 호출 실패 memberId={}: {}", memberId, e.getMessage());
            return List.of();
        }
    }

    private double evaluateTriggers(Long memberId, MemberStats stats, int currentStreak,
                                     LocalDate today, LocalTime now, List<String> triggered) {
        // STREAK_RISK: DailyGoal 미달성 + streak > 0 + 17:00 이후
        boolean goalAchieved = dailyGoalRepository.findByMemberIdAndDate(memberId, today)
                .map(DailyGoal::isAchieved).orElse(false);
        if (!goalAchieved && currentStreak > 0 && now.isAfter(LocalTime.of(17, 0))) {
            triggered.add("STREAK_RISK");
        }

        // MEAL_REMINDER: 칼로리 달성률 < 50% + 12:00 이후
        Optional<Program> programOpt = programRepository.findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE);
        double targetKcal = programOpt.map(p -> (double) p.getTargetCalories()).orElse(0.0);
        double todayKcal = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(memberId, today);
        double kcalRate = targetKcal > 0 ? todayKcal / targetKcal : 0.0;
        if (kcalRate < 0.5 && now.isAfter(LocalTime.of(12, 0))) {
            triggered.add("MEAL_REMINDER");
        }

        // EXERCISE_REMINDER: 이번 주 세션 없음 + 루틴 등록됨 + 17:00 이후
        boolean hasRoutine = routineRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId).isPresent();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        int thisWeekSessions = routineSessionRepository
                .countDistinctSessionDatesByMemberIdAndDateBetween(memberId, weekStart, today);
        if (hasRoutine && thisWeekSessions == 0 && now.isAfter(LocalTime.of(17, 0))) {
            triggered.add("EXERCISE_REMINDER");
        }

        // WEEKLY_REPORT_READY: content != null + filledAt 7일 이내
        programOpt.flatMap(p ->
                weeklyReportRepository.findFirstByProgramIdAndContentIsNotNullOrderByWeekNumberDesc(p.getId())
        ).filter(wr ->
                wr.getFilledAt() != null && wr.getFilledAt().isAfter(LocalDateTime.now().minusDays(7))
        ).ifPresent(wr -> triggered.add("WEEKLY_REPORT_READY"));

        return kcalRate;
    }

    private void cacheResult(String key, List<NotificationResult> result) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("알림 캐시 저장 실패 key={}: {}", key, e.getMessage());
        }
    }
}
```

- [ ] **Step 5: 테스트 실행 (통과 확인)**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.notification.application.NotificationServiceTest" -q
```

Expected: 4 passed

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/notification/application/ \
        backend/src/test/java/com/ssafy/manager/notification/application/
git commit -m "feat(notification): NotificationService 트리거 평가 + Redis 캐시"
```

---

### Task 5: NotificationController — GET /notifications

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/notification/presentation/dto/NotificationResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/notification/presentation/NotificationController.java`
- Create: `backend/src/test/java/com/ssafy/manager/notification/presentation/NotificationControllerTest.java`

**Interfaces:**
- Consumes: `NotificationService.getNotifications(Long memberId): List<NotificationResult>`
- Produces: `GET /notifications` → `ResponseEntity<List<NotificationResponse>>`

- [ ] **Step 1: NotificationResponse.java 작성**

```java
package com.ssafy.manager.notification.presentation.dto;

import com.ssafy.manager.notification.application.NotificationResult;

public record NotificationResponse(
        String type,
        String message,
        String recommendedTime,
        int priority
) {
    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.type(),
                result.message(),
                result.recommendedTime(),
                result.priority()
        );
    }
}
```

- [ ] **Step 2: 실패 테스트 작성**

`backend/src/test/java/com/ssafy/manager/notification/presentation/NotificationControllerTest.java`:
```java
package com.ssafy.manager.notification.presentation;

import com.ssafy.manager.auth.KakaoOAuth2UserService;
import com.ssafy.manager.auth.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.notification.application.NotificationResult;
import com.ssafy.manager.notification.application.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean NotificationService notificationService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    private static final Long MEMBER_ID = 1L;
    private static final Authentication AUTH =
            new UsernamePasswordAuthenticationToken(MEMBER_ID, null, List.of());

    @Test
    void 알림_목록을_반환한다() throws Exception {
        given(notificationService.getNotifications(MEMBER_ID)).willReturn(List.of(
                new NotificationResult("STREAK_RISK", "오늘 Streak이 끊길 위험이에요!", "20:00", 1)
        ));

        mockMvc.perform(get("/notifications").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("STREAK_RISK"))
                .andExpect(jsonPath("$[0].message").value("오늘 Streak이 끊길 위험이에요!"))
                .andExpect(jsonPath("$[0].recommendedTime").value("20:00"))
                .andExpect(jsonPath("$[0].priority").value(1));
    }

    @Test
    void 알림이_없으면_빈_배열을_반환한다() throws Exception {
        given(notificationService.getNotifications(MEMBER_ID)).willReturn(List.of());

        mockMvc.perform(get("/notifications").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 미인증_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: 테스트 실행 (실패 확인)**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.notification.presentation.NotificationControllerTest" -q
```

Expected: FAIL (NotificationController 없음)

- [ ] **Step 4: NotificationController.java 작성**

```java
package com.ssafy.manager.notification.presentation;

import com.ssafy.manager.notification.application.NotificationService;
import com.ssafy.manager.notification.presentation.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long memberId) {
        List<NotificationResponse> response = notificationService.getNotifications(memberId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 5: 테스트 실행 (통과 확인)**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.notification.presentation.NotificationControllerTest" -q
```

Expected: 3 passed

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/notification/presentation/ \
        backend/src/test/java/com/ssafy/manager/notification/presentation/
git commit -m "feat(notification): GET /notifications 컨트롤러 구현"
```

---

### Task 6: 캐시 무효화 — MealService + RoutineSessionService

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/application/MealService.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java`

**Interfaces:**
- Consumes: `NotificationService.evictCache(Long memberId): void`
- Produces: 식단/운동 저장 후 Redis 캐시 자동 무효화

- [ ] **Step 1: MealService에 NotificationService 주입 + evictCache 호출**

`MealService.java`에 필드 추가 (`@RequiredArgsConstructor`가 있으므로 final 필드로 선언):
```java
private final NotificationService notificationService;
```

`record()` 메서드 마지막 `return meal;` 직전에 추가:
```java
notificationService.evictCache(command.memberId());
return meal;
```

`addItem()` 메서드 마지막 `return meal;` 직전에 추가:
```java
notificationService.evictCache(memberId);
return meal;
```

`recordFromPhoto()` 메서드 마지막 `return meal;` 직전에 추가:
```java
notificationService.evictCache(command.memberId());
return meal;
```

- [ ] **Step 2: RoutineSessionService에 NotificationService 주입 + evictCache 호출**

`RoutineSessionService.java`에 필드 추가:
```java
private final NotificationService notificationService;
```

`saveSessionAndSets()` 메서드의 `return RoutineSessionResult.from(session, sets);` 직전에 추가:
```java
notificationService.evictCache(memberId);
return RoutineSessionResult.from(session, sets);
```

- [ ] **Step 3: 빌드 + 전체 테스트**

```bash
cd backend && ./gradlew test -q
```

Expected: BUILD SUCCESSFUL, 기존 테스트 모두 통과 (MealServiceTest, RoutineSessionServiceTest 포함)

> 기존 테스트에서 `@Mock NotificationService notificationService` + `doNothing().when(notificationService).evictCache(any())` 스텁이 필요할 수 있음. 테스트 실패 시 해당 Mock 추가.

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/nutrition/application/MealService.java \
        backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java
git commit -m "feat(notification): 식단/운동 저장 후 알림 캐시 무효화"
```

---

### Task 7: Vue — api + composable + NotificationBell + App.vue

**Files:**
- Create: `frontend/src/api/notification.js`
- Create: `frontend/src/composables/useNotifications.js`
- Create: `frontend/src/components/NotificationBell.vue`
- Modify: `frontend/src/App.vue`

**Interfaces:**
- Produces: `useNotifications()` → `{ notifications: Ref<NotificationItem[]>, fetch: () => Promise<void> }`
- Produces: `<NotificationBell />` 컴포넌트 (헤더에 삽입)

- [ ] **Step 1: notification.js API 함수 작성**

`frontend/src/api/notification.js`:
```js
import { apiClient } from '@/services/apiClient';

export function getNotifications() {
  return apiClient.get('/notifications');
}
```

- [ ] **Step 2: useNotifications.js composable 작성**

`frontend/src/composables/useNotifications.js`:
```js
import { ref } from 'vue';
import { getNotifications } from '@/api/notification';

// 모듈 싱글톤 — 어느 컴포넌트에서 호출해도 같은 ref를 공유
const notifications = ref([]);

export function useNotifications() {
  async function fetch() {
    try {
      const data = await getNotifications();
      notifications.value = Array.isArray(data) ? data : [];
    } catch {
      notifications.value = [];
    }
  }

  return { notifications, fetch };
}
```

- [ ] **Step 3: NotificationBell.vue 작성**

`frontend/src/components/NotificationBell.vue`:
```vue
<template>
  <div class="relative">
    <button
      class="relative p-2 border-2 border-black bg-white hover:bg-yellow-300 transition-colors"
      @click="toggleOpen"
    >
      <span class="text-lg">🔔</span>
      <span
        v-if="unreadCount > 0"
        class="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center border border-black"
      >{{ unreadCount }}</span>
    </button>

    <div
      v-if="isOpen && notifications.length > 0"
      class="absolute right-0 top-full mt-1 w-80 bg-white border-2 border-black shadow-brutal z-50"
    >
      <div
        v-for="n in notifications"
        :key="n.type"
        class="p-3 border-b border-black last:border-b-0 hover:bg-yellow-50"
      >
        <div class="flex items-center gap-2 mb-1">
          <span>{{ typeIcon(n.type) }}</span>
          <span class="font-bold text-sm">{{ typeLabel(n.type) }}</span>
          <span class="ml-auto text-xs text-gray-500">{{ n.recommendedTime === 'now' ? '지금' : n.recommendedTime }}</span>
        </div>
        <p class="text-sm text-gray-700">{{ n.message }}</p>
      </div>
    </div>

    <div
      v-if="isOpen && notifications.length === 0"
      class="absolute right-0 top-full mt-1 w-64 bg-white border-2 border-black p-3 shadow-brutal z-50"
    >
      <p class="text-sm text-gray-500 text-center">새 알림이 없어요 ✅</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useNotifications } from '@/composables/useNotifications';

const { notifications } = useNotifications();
const isOpen = ref(false);

const unreadCount = computed(() => notifications.value.length);

function toggleOpen() {
  isOpen.value = !isOpen.value;
}

function typeIcon(type) {
  const icons = {
    STREAK_RISK: '🔴',
    MEAL_REMINDER: '🍽️',
    EXERCISE_REMINDER: '💪',
    WEEKLY_REPORT_READY: '📊',
  };
  return icons[type] ?? '🔔';
}

function typeLabel(type) {
  const labels = {
    STREAK_RISK: 'Streak 위기',
    MEAL_REMINDER: '식사 리마인더',
    EXERCISE_REMINDER: '운동 리마인더',
    WEEKLY_REPORT_READY: '주간 리포트',
  };
  return labels[type] ?? type;
}
</script>
```

- [ ] **Step 4: App.vue 수정**

`frontend/src/App.vue`를 열어 헤더 영역에 `<NotificationBell />` 추가 및 `onMounted` fetch 연결.

기존 `<script setup>` 블록에 추가:
```js
import { onMounted } from 'vue';
import { useNotifications } from '@/composables/useNotifications';
import NotificationBell from '@/components/NotificationBell.vue';

const { fetch: fetchNotifications } = useNotifications();

onMounted(() => {
  fetchNotifications();
});
```

헤더 최상단 우측 영역(로그인 버튼 근처)에 추가:
```html
<NotificationBell />
```

- [ ] **Step 5: 식단 저장 후 알림 갱신 연결**

식단을 저장하는 뷰(LogView.vue)에서 저장 완료 후 `fetchNotifications()` 호출:
```js
import { useNotifications } from '@/composables/useNotifications';
const { fetch: fetchNotifications } = useNotifications();

// 기존 식단 저장 함수 완료 후 호출
await saveMeal(...)
fetchNotifications();
```

운동 세션을 저장하는 뷰(RoutineView 또는 관련 뷰)에서도 동일하게 추가.

- [ ] **Step 6: 개발 서버 동작 확인**

```bash
cd frontend && npm run dev
```

1. 앱 접속 후 헤더 우측에 🔔 벨 아이콘 확인
2. 벨 클릭 시 드롭다운 열림 확인
3. 로그인 후 `/notifications` API 호출 확인 (브라우저 개발자도구 → Network)
4. 알림 있을 때 빨간 배지 숫자 표시 확인

- [ ] **Step 7: 커밋**

```bash
git add frontend/src/api/notification.js \
        frontend/src/composables/useNotifications.js \
        frontend/src/components/NotificationBell.vue \
        frontend/src/App.vue
git commit -m "feat(notification): Vue 알림 벨 컴포넌트 + composable 구현"
```
