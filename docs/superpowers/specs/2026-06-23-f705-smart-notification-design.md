# F705 스마트 알림 타이밍 설계 스펙

## 목표

사용자가 앱을 열면, 현재 상태(Streak 위기·식사 미기록·운동 미기록·주간 리포트 생성)를 실시간으로 평가해 Claude가 맥락에 맞는 메시지와 권장 발송 시각을 생성하는 인앱 알림 시스템.

## 범위 제외

- Web Push(Service Worker, VAPID) — 인앱 알림만 구현
- 모바일 — 웹 기준만 구현
- 읽음 처리 — 알림은 stateless(Redis), 읽음 상태 DB 저장 없음
- 사용자 행동 기반 개인 타이밍 — 이번 구현은 HealthGoal 기반. `Meal.recordedAt`은 향후를 위해 적재 시작

---

## 아키텍처

```
Vue → GET /notifications (JWT 인증)
        Spring NotificationService
          ① Redis 조회  key: notifications:{memberId}  TTL: 1h
          ② HIT  → 캐시 반환
          ③ MISS → 트리거 조건 평가 (규칙 기반, DB 쿼리)
                  → triggered_types 결정
                  → triggered_types 비어있으면 빈 목록 즉시 반환
                  → POST /ai/notification/timing (FastAPI)
                  → 메시지 + recommended_time 수신
                  → Redis SET (TTL 1h)
                  → 반환

캐시 무효화:
  MealService.save()           → Redis.delete("notifications:{memberId}")
  RoutineSessionService.save() → Redis.delete("notifications:{memberId}")

Vue: 식단/운동 저장 완료 후 → GET /notifications 재호출
```

---

## 알림 유형 4가지

| type | 트리거 조건 | 시각 조건 |
|---|---|---|
| `STREAK_RISK` | 오늘 DailyGoal 미달성 + currentStreak > 0 | 현재 시각 17:00 이후 |
| `MEAL_REMINDER` | 오늘 칼로리 달성률 < 50% | 현재 시각 12:00 이후 |
| `EXERCISE_REMINDER` | 이번 주 RoutineSession 없음 + Routine 등록됨 | 현재 시각 17:00 이후 |
| `WEEKLY_REPORT_READY` | 최신 WeeklyReport.content != null + 생성 7일 이내 | 시각 무관 (항상 표시) |

---

## FastAPI 엔드포인트

### `POST /ai/notification/timing`

**요청:**
```json
{
  "member_id": 1,
  "health_goal": "MUSCLE",
  "current_streak": 5,
  "triggered_types": ["STREAK_RISK", "EXERCISE_REMINDER"],
  "context": {
    "today_kcal_rate": 0.36,
    "meal_count": 1,
    "this_week_sessions": 0
  }
}
```

**응답:**
```json
{
  "notifications": [
    {
      "type": "STREAK_RISK",
      "message": "5일 연속 달성 중이에요! 오늘 저녁 식단을 기록하면 Streak를 지킬 수 있어요.",
      "recommended_time": "20:00",
      "priority": 1
    },
    {
      "type": "EXERCISE_REMINDER",
      "message": "이번 주 아직 운동을 시작하지 않으셨네요. 오늘 루틴을 시작해볼까요?",
      "recommended_time": "18:00",
      "priority": 2
    }
  ]
}
```

**Claude 역할:**
- HealthGoal + currentStreak + context를 기반으로 메시지 개인화
- recommended_time: HealthGoal별 기준 시각 ± 조정 (DIET → 식사 전 강조, MUSCLE → 운동 전 강조)
- mock 모드(`ENV=dev`): triggered_types별 고정 메시지 반환, Claude 미호출

**패키지 위치:**
- `ai/app/routers/ai_notification.py`
- `ai/app/services/notification_service.py`
- `ai/app/schemas/notification.py`

---

## Spring 레이어

### 패키지 구조

```
com.ssafy.manager.notification/
  presentation/
    NotificationController          GET /notifications
    dto/
      NotificationResponse          응답 DTO
  application/
    NotificationService             트리거 평가 + 캐시 + FastAPI 호출
  infrastructure/
    client/
      AiNotificationClient          POST /ai/notification/timing
      AiNotificationClientRequest
      AiNotificationClientResponse
```

### NotificationController

```
GET /notifications
  @AuthenticationPrincipal Long memberId
  → ResponseEntity<List<NotificationResponse>>
```

### NotificationService 흐름

1. Redis 조회 (`notifications:{memberId}`)
2. HIT → deserialize + 반환
3. MISS:
   - `DailyGoalRepository`: 오늘 DailyGoal achieved 여부 + MemberStats currentStreak
   - `MealItemRepository`: 오늘 calorie 합산 + meal_count
   - `RoutineSessionRepository`: 이번 주 세션 수
   - `RoutineRepository`: 루틴 존재 여부
   - `WeeklyReportRepository`: 최신 리포트 content + createdAt
   - triggered_types 수집
   - triggered_types 비어있으면 빈 목록 → Redis SET → 반환
   - `AiNotificationClient.requestTiming(req)` 호출
   - 결과 Redis SET (TTL 1h) → 반환

### 캐시 무효화

`MealService.save()` 완료 후:
```java
redisTemplate.delete("notifications:" + memberId);
```

`RoutineSessionService.save()` 완료 후 동일.

### RestClient 빈

`RestClientConfig`에 `aiNotificationRestClient` 추가 (readTimeout 15s).

---

## 데이터 모델 변경

### Meal 엔티티 컬럼 추가

```java
private LocalDateTime recordedAt;  // 향후 패턴 분석용, 현 구현에서는 저장만
```

`MealService.save()` 시 `LocalDateTime.now()` 자동 세팅. 이번 F705 로직에서는 사용하지 않음.

### WeeklyReport 엔티티 컬럼 추가

```java
private LocalDateTime filledAt;  // AI 내용이 채워진 시각
```

`WeeklyReport.fill()` 호출 시 `LocalDateTime.now()` 세팅. `WEEKLY_REPORT_READY` 트리거에서 "생성 7일 이내" 판단에 사용.

---

## Vue 레이어

### 파일 구조

```
src/
  api/notification.js           getNotifications()
  stores/notificationStore.js   알림 목록 + unreadCount (Pinia)
  components/NotificationBell.vue  벨 아이콘 + 드롭다운
```

### 동작

- `App.vue` `onMounted` → `notificationStore.fetch()`
- 식단 저장(`MealStore`) 완료 후 → `notificationStore.fetch()`
- 운동 저장(`RoutineSessionStore`) 완료 후 → `notificationStore.fetch()`
- `NotificationBell`: 알림 개수 배지 + 드롭다운 목록 (type별 아이콘, message, recommended_time 표시)

---

## 에러 처리

| 상황 | 처리 방식 |
|---|---|
| FastAPI 타임아웃/오류 | `catch (Exception e)` → `log.warn` → 빈 목록 반환 (500 전파 없음) |
| Redis 연결 오류 | try/catch → 캐시 건너뛰고 FastAPI 직접 호출 |
| `triggered_types` 빈 배열 | FastAPI 호출 없이 즉시 빈 목록 반환 |
| triggered_types 있는데 FastAPI 실패 | 빈 목록 반환 (캐시 저장 안 함, 다음 요청에서 재시도) |

---

## 테스트 범위

**Spring:**
- `NotificationServiceTest`: 트리거 조건 4가지 각각 + FastAPI 실패 시 빈 목록 + triggered_types 빈 배열 시 FastAPI 미호출
- `NotificationControllerTest`: 200 정상 / 401 미인증

**FastAPI:**
- `test_notification.py`: triggered_types별 메시지 생성 + mock 모드 응답
