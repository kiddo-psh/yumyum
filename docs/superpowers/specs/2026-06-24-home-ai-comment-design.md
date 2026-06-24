# 홈화면 AI 개인화 코멘트 설계 스펙

## 목표

`HomeView.vue` 상단 냠냠 캐릭터 옆 `coachMessage`를 Claude가 생성한 개인화 코멘트로 교체한다.
현재 정적 텍스트(남은 칼로리 수치)를 HealthGoal 기반 동기부여 한 줄 메시지로 대체한다.

## 범위 제외

- 영양소 수치 기반 음식 추천 — 코멘트는 목표 중심, 음식 제안 없음
- 알림(FCM, Web Push) — 인앱 홈화면 표시만
- 코멘트 이력 저장 — stateless (Redis TTL만 사용)

---

## 아키텍처

```
Vue (HomeView 마운트)
  → GET /home/comment  (Spring, JWT 인증)
      ① Redis 조회  key: home_comment:{memberId}  TTL: 12h
      ② HIT  → 캐시 반환
      ③ MISS → DailySummary + MemberStats 조회 (DB)
             → POST /ai/home/comment (FastAPI)
             → Redis SET TTL 12h
             → 반환
```

**갱신 주기:** TTL 12h — 마지막 생성 후 12시간 경과 시 다음 방문에 재생성

---

## FastAPI 엔드포인트

### `POST /ai/home/comment`

**요청:**
```json
{
  "member_id": 1,
  "health_goal": "MUSCLE",
  "current_streak": 5,
  "kcal_rate": 0.62,
  "remaining_kcal": 680,
  "protein_g": 45.0,
  "carb_g": 120.0,
  "fat_g": 28.0
}
```

**응답:**
```json
{
  "comment": "근육 증가 목표, 오늘 62% 달성 중이에요. Streak 5일째 잘 하고 있어요!"
}
```

**Claude 프롬프트 방향:**
- HealthGoal이 코멘트의 중심 (DIET → 칼로리/체중 감량 톤, MUSCLE → 근육 증가 톤, HEALTH → 건강 유지 톤, DISEASE → 안전한 관리 톤)
- 칼로리 달성률 + Streak로 오늘 상태 표현
- 영양소 수치와 음식 추천은 언급하지 않음
- 한 줄, 구어체, 한국어, 100자 이내

**mock 모드 (`ENV=dev`):** HealthGoal별 고정 코멘트 반환, Claude 미호출

**패키지 위치:**
```
ai/app/routers/ai_home.py
ai/app/services/home_comment_service.py
ai/app/schemas/home.py
```

---

## Spring 레이어

### 패키지 구조

```
com.ssafy.manager.home/
  presentation/
    HomeController             GET /home/comment
    dto/
      HomeCommentResponse
  application/
    HomeCommentService         Redis 캐싱 + FastAPI 호출
  infrastructure/
    client/
      AiHomeCommentClient
      AiHomeCommentClientRequest
      AiHomeCommentClientResponse
```

### HomeController

```
GET /home/comment
  @AuthenticationPrincipal Long memberId
  → ResponseEntity<HomeCommentResponse>
```

### HomeCommentService 흐름

1. Redis 조회 (`home_comment:{memberId}`)
2. HIT → 반환
3. MISS:
   - `DailySummaryService`: 오늘 칼로리 달성률, 탄단지 섭취량
   - `MemberStatsRepository`: currentStreak, HealthGoal
   - `AiHomeCommentClient.request(req)` 호출
   - Redis SET (TTL 12h)
   - 반환

### RestClient 빈

`RestClientConfig`에 `aiHomeCommentRestClient` 추가 (readTimeout 15s).

---

## Vue 레이어

### 변경 파일

```
src/api/dashboard.js        getHomeComment() 함수 추가
src/views/HomeView.vue      coachMessage 정적 computed → API 호출로 교체
```

### 동작

- `onMounted` → `getHomeComment()` 호출
- 로딩 중: 기존 정적 메시지(`남은 칼로리 X kcal`) 유지
- 응답 수신 후: `coachMessage` 교체
- API 실패 시: 기존 정적 메시지 그대로 표시 (사용자에게 오류 노출 없음)

---

## 에러 처리

| 상황 | 처리 |
|---|---|
| FastAPI 타임아웃/오류 | `log.warn` → fallback `"오늘도 건강한 하루 보내세요!"` 반환 |
| Redis 오류 | 캐시 건너뛰고 FastAPI 직접 호출 |
| DailySummary 조회 실패 | 기본값(streak=0, kcal_rate=0)으로 FastAPI 호출 |
| Vue API 실패 | 정적 coachMessage 유지 (에러 표시 없음) |

---

## 테스트 범위

**Spring:**
- `HomeCommentServiceTest`
  - Redis HIT → FastAPI 미호출 검증
  - Redis MISS → FastAPI 호출 + Redis 저장 검증
  - FastAPI 실패 → fallback 반환 검증
- `HomeControllerTest`
  - 200 정상 / 401 미인증

**FastAPI:**
- `test_home_comment.py`
  - HealthGoal별 코멘트 생성 (DIET/MUSCLE/HEALTH/DISEASE)
  - mock 모드 응답 검증
