# F704 Multi-Agent 주간 코칭 설계 스펙

**Date:** 2026-06-23
**Feature:** 순차 체인 Multi-Agent로 식단·운동·체중 추세를 통합 분석하는 주간 코칭
**Scope:** FastAPI 전용 (Spring 연동 포인트 #5 URL 변경 포함)

---

## 배경 및 목표

기존 `/ai/report/weekly`는 모든 데이터를 단일 Claude 프롬프트에 넣어 1회 호출하는 구조다.
영양·운동·체중이라는 서로 다른 도메인을 한 번에 분석하다 보니 교차 추론(예: "단백질 부족이 근력 정체의 원인")이 약하다.

F704는 전문 Agent 3개를 순차로 연결하고 Synthesis Agent가 종합하는 구조로 대체한다.
각 Agent가 이전 Agent의 분석을 컨텍스트로 받아 더 정교한 교차 추론이 가능해진다.

---

## 배치 실행 모델

```
[Spring @Scheduled — 월요일 새벽]
  1. 지난 주 Program 완료 회원 목록 조회
  2. 회원별 POST /ai/coaching/weekly 호출 (FastAPI)
  3. 결과를 WeeklyReport 테이블에 저장

[월요일 오전 9시]
  4. 회원이 앱 열면 저장된 WeeklyReport 조회해서 표시
```

배치 실행이므로 지연 최적화보다 품질이 우선이다. 순차 체인을 채택한 이유.

---

## 전체 데이터 흐름

```
Spring → POST /ai/coaching/weekly (WeeklyCoachingRequest)
              ↓
         coaching_service.py 순차 체인
              ↓
    ┌─── 영양 Agent ──────────────────────────────┐
    │  입력: daily_nutrition[] + targets           │
    │  출력: nutrition_analysis (str)              │
    │  예: "칼로리 달성률 82%, 단백질 68%로 부족..." │
    └─────────────────────────────────────────────┘
              ↓ nutrition_analysis 전달
    ┌─── 운동 Agent ──────────────────────────────┐
    │  입력: routine_sessions[] + nutrition_analysis│
    │  출력: exercise_analysis (str)               │
    │  예: "2/3 세션 성공. 단백질 부족 시 근력 정체  │
    │       가능성 있음..."                         │
    └─────────────────────────────────────────────┘
              ↓ exercise_analysis 전달
    ┌─── 목표 Agent ──────────────────────────────┐
    │  입력: weight_records[] + health_goal        │
    │        + nutrition_analysis + exercise_analysis│
    │  출력: goal_analysis (str)                   │
    │  예: "체중 -0.4kg/주, MUSCLE 목표 대비 감량   │
    │       속도 과도. 칼로리 부족이 원인..."         │
    └─────────────────────────────────────────────┘
              ↓ 세 분석 전달
    ┌─── Synthesis ───────────────────────────────┐
    │  입력: nutrition_analysis + exercise_analysis │
    │        + goal_analysis                       │
    │  출력: ai_comment (4~5문장 한국어 코칭)        │
    └─────────────────────────────────────────────┘
              ↓
Spring ← WeeklyCoachingResponse
```

---

## API 계약

### 엔드포인트

`POST /ai/coaching/weekly`

기존 `/ai/report/weekly` 대체. Spring 연동 포인트 #5 URL 변경 필요.

### 요청 스키마

```python
class DailyNutritionRecord(BaseModel):
    date: str                # "2026-06-16"
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    calories_burned: float   # 운동 소모 칼로리

class RoutineSessionRecord(BaseModel):
    exercise_name: str
    successful_sets: int
    total_sets: int
    weight_kg: float
    session_date: str

class WeightRecord(BaseModel):
    date: str
    weight_kg: float

class WeeklyCoachingRequest(BaseModel):
    week_number: int
    health_goal: str              # DIET / MUSCLE / HEALTH / DISEASE
    daily_nutrition: List[DailyNutritionRecord]
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    routine_sessions: List[RoutineSessionRecord]
    weight_records: List[WeightRecord]
```

### 응답 스키마

```python
class WeeklyCoachingResponse(BaseModel):
    ai_comment: str           # 최종 통합 코칭 텍스트 (Vue 표시용)
    nutrition_summary: str    # 영양 Agent 분석 (DB 저장 + 상세 탭용)
    exercise_summary: str     # 운동 Agent 분석
    goal_summary: str         # 목표 Agent 분석
    avg_calorie_rate: float   # 칼로리 달성률 평균 (%)
    achievement_days: int     # 목표 달성일 수 (80~120% 범위)
    weight_trend: float | None  # kg/주, 기록 없으면 null
```

---

## 파일 구조

### 신규 (FastAPI)

| 파일 | 역할 |
|---|---|
| `ai/app/schemas/coaching.py` | WeeklyCoachingRequest/Response + 중첩 모델 |
| `ai/app/services/coaching_service.py` | 4단계 순차 체인 오케스트레이션 + 수치 계산 |
| `ai/app/routers/ai_coaching.py` | POST /ai/coaching/weekly |
| `ai/tests/test_coaching_service.py` | 체인 단위 테스트 |
| `ai/tests/test_ai_coaching.py` | 엔드포인트 통합 테스트 |

### 수정 (FastAPI)

| 파일 | 변경 내용 |
|---|---|
| `ai/app/main.py` | ai_coaching 라우터 등록 |
| `ai/app/services/claude_service.py` | weekly-coaching mock 분기 추가 |

### 기존 유지

`ai/app/routers/ai_report.py` — `/ai/report/weekly` 그대로 유지  
(Spring 연동 URL 변경은 Spring 담당자 작업. FastAPI는 새 엔드포인트만 추가)

---

## 에러 처리

Agent 하나가 실패해도 배치 전체가 중단되지 않도록 fallback 텍스트로 체인을 계속 진행한다.

```python
try:
    nutrition_analysis = await call_claude(nutrition_prompt)
except Exception:
    nutrition_analysis = "영양 분석 불가"

# 이하 동일 패턴. Synthesis는 수신한 분석 텍스트 그대로 사용.
# ai_comment에 "일부 분석에 문제가 있었습니다" 문구 자동 포함.
```

Spring은 항상 200 OK를 받고 WeeklyReport를 저장한다.

---

## 테스트 전략

### test_coaching_service.py (단위)

- 영양 Agent 프롬프트에 `avg_calorie_rate`, `health_goal` 포함 확인
- 운동 Agent 프롬프트에 `nutrition_analysis` 전달 확인
- 목표 Agent 프롬프트에 `weight_trend` 수치 포함 확인
- Agent 실패 시 fallback 텍스트로 체인 계속 진행 확인

### test_ai_coaching.py (통합)

- 200 응답 + 응답 필드 7개 전부 존재 확인
- `weight_records` 빈 리스트 → `weight_trend = null`
- `routine_sessions` 빈 리스트 → 정상 처리
- `ENV=dev` mock 응답 반환 확인

---

## Mock 응답 (ENV=dev)

`claude_service.py`에 기존 패턴대로 분기 추가:

`coaching_service.py`의 각 Agent 호출 함수가 직접 mock 텍스트를 반환:

```python
# coaching_service.py
async def _nutrition_agent(data) -> str:
    if settings.env == "dev":
        return "[MOCK] 칼로리 달성률 85%, 단백질 섭취 다소 부족합니다."
    return await call_claude(nutrition_prompt)

async def _exercise_agent(data, nutrition_analysis) -> str:
    if settings.env == "dev":
        return "[MOCK] 세션 성공률 양호, 단백질 보충 권장합니다."
    return await call_claude(exercise_prompt)

async def _goal_agent(data, nutrition_analysis, exercise_analysis) -> str:
    if settings.env == "dev":
        return "[MOCK] 체중 추세 안정적, 현재 궤도 유지하세요."
    return await call_claude(goal_prompt)

async def _synthesis_agent(nutrition, exercise, goal) -> str:
    if settings.env == "dev":
        return "[MOCK] 이번 주 전반적으로 잘 하셨습니다. 단백질 섭취를 조금 늘리면 더욱 효과적입니다."
    return await call_claude(synthesis_prompt)
```

---

## 모델

`claude-haiku-4-5` — 각 Agent는 분석 텍스트 생성이 목적이므로 haiku로 충분.  
Synthesis도 haiku. (CLAUDE.md 기준 opus는 RAG/Multi-Agent용이나, 배치 비용 절감 목적으로 haiku 채택)

> 추후 품질이 부족하면 Synthesis만 opus로 업그레이드 가능.
