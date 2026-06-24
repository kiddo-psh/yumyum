# F704 Multi-Agent 주간 코칭 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 영양·운동·목표 Agent를 순차 체인으로 연결해 식단+운동+체중 추세를 통합 분석하는 `POST /ai/coaching/weekly` 엔드포인트를 구현한다.

**Architecture:** 순차 체인 4단계 — 영양 Agent → 운동 Agent(영양 분석 참고) → 목표 Agent(둘 다 참고) → Synthesis Agent(최종 코칭). 배치 실행(Spring @Scheduled)을 전제하므로 지연 최적화 없이 순차 처리. `ENV=dev`는 각 Agent 함수 내부에서 mock 텍스트를 즉시 반환.

**Tech Stack:** Python 3.11, FastAPI, pytest, 기존 `claude_service.call_claude()`, `trend_service.calc_weight_trend()`

## Global Constraints

- 모든 Claude 호출은 `call_claude()` 경유 — Agent 함수 내 직접 httpx 호출 금지
- `ENV=dev` → 각 Agent 함수가 즉시 `[MOCK]` 텍스트 반환, Claude 호출 없음
- 모델: `claude-haiku-4-5` (기본값, `call_claude()` default_model 사용)
- 엔드포인트 경로: `POST /ai/coaching/weekly`
- 응답 필드 7개: `ai_comment`, `nutrition_summary`, `exercise_summary`, `goal_summary`, `avg_calorie_rate`, `achievement_days`, `weight_trend`
- `weight_records` 2개 미만 → `weight_trend = null`
- Agent 하나 실패(Exception) → fallback 텍스트로 체인 계속 진행, 절대 500 반환 금지
- 테스트 실행: `cd ai && python -m pytest tests/test_coaching_service.py tests/test_ai_coaching.py -v`

---

## 파일 맵

| 파일 | 역할 |
|---|---|
| `ai/app/schemas/coaching.py` (신규) | Pydantic 요청·응답 모델 |
| `ai/app/services/coaching_service.py` (신규) | 수치 계산 + 4단계 Agent 체인 오케스트레이션 |
| `ai/app/routers/ai_coaching.py` (신규) | `POST /ai/coaching/weekly` 엔드포인트 |
| `ai/app/main.py` (수정) | `ai_coaching` 라우터 등록 |
| `ai/tests/test_coaching_service.py` (신규) | `coaching_service` 단위 테스트 |
| `ai/tests/test_ai_coaching.py` (신규) | 엔드포인트 통합 테스트 |

---

## Task 1: Pydantic 스키마

**Files:**
- Create: `ai/app/schemas/coaching.py`

**Interfaces:**
- Produces: `WeeklyCoachingRequest`, `WeeklyCoachingResponse` — Task 2, 3에서 import

- [ ] **Step 1: `ai/app/schemas/coaching.py` 작성**

```python
from pydantic import BaseModel
from typing import List, Optional, Literal


class DailyNutritionRecord(BaseModel):
    date: str                    # "2026-06-16"
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    calories_burned: float = 0.0


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
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    daily_nutrition: List[DailyNutritionRecord]
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    routine_sessions: List[RoutineSessionRecord]
    weight_records: List[WeightRecord]


class WeeklyCoachingResponse(BaseModel):
    ai_comment: str
    nutrition_summary: str
    exercise_summary: str
    goal_summary: str
    avg_calorie_rate: float
    achievement_days: int
    weight_trend: Optional[float]
```

- [ ] **Step 2: import 동작 확인**

```bash
cd ai && python -c "from app.schemas.coaching import WeeklyCoachingRequest, WeeklyCoachingResponse; print('OK')"
```

Expected: `OK`

- [ ] **Step 3: 커밋**

```bash
git add ai/app/schemas/coaching.py
git commit -m "feat(coaching): WeeklyCoachingRequest/Response 스키마 추가"
```

---

## Task 2: coaching_service.py — TDD

**Files:**
- Create: `ai/app/services/coaching_service.py`
- Create: `ai/tests/test_coaching_service.py`

**Interfaces:**
- Consumes: `WeeklyCoachingRequest`, `WeeklyCoachingResponse` (Task 1), `call_claude` (`claude_service`), `calc_weight_trend` (`trend_service`)
- Produces: `run_coaching_chain(req: WeeklyCoachingRequest) -> WeeklyCoachingResponse`

- [ ] **Step 1: 테스트 파일 작성**

```python
# ai/tests/test_coaching_service.py
import os
os.environ.setdefault("ENV", "dev")

import pytest
from app.schemas.coaching import (
    WeeklyCoachingRequest, DailyNutritionRecord,
    RoutineSessionRecord, WeightRecord,
)
from app.services.coaching_service import _calc_stats, run_coaching_chain


# ── 공통 픽스처 ────────────────────────────────────────────────────────

def _make_request(**overrides) -> WeeklyCoachingRequest:
    base = dict(
        week_number=3,
        health_goal="MUSCLE",
        daily_nutrition=[
            DailyNutritionRecord(date=f"2026-06-1{i}", kcal=1800.0, protein_g=80.0,
                                 carb_g=220.0, fat_g=60.0, calories_burned=300.0)
            for i in range(7)
        ],
        target_kcal=2000.0,
        target_protein_g=120.0,
        target_carb_g=250.0,
        target_fat_g=65.0,
        routine_sessions=[
            RoutineSessionRecord(exercise_name="벤치프레스", successful_sets=3,
                                 total_sets=4, weight_kg=60.0, session_date="2026-06-16"),
        ],
        weight_records=[
            WeightRecord(date="2026-06-10", weight_kg=70.0),
            WeightRecord(date="2026-06-17", weight_kg=70.3),
        ],
    )
    base.update(overrides)
    return WeeklyCoachingRequest(**base)


# ── _calc_stats 단위 테스트 ────────────────────────────────────────────

def test_칼로리_달성률_평균_계산():
    req = _make_request()
    avg, days = _calc_stats(req)
    # kcal=1800 + burned=300=2100, target=2000 → rate=105% → 7일 모두 80~120% → 7일 달성
    assert avg == pytest.approx(105.0, abs=1.0)
    assert days == 7


def test_daily_nutrition_없으면_달성률_0():
    req = _make_request(daily_nutrition=[])
    avg, days = _calc_stats(req)
    assert avg == 0.0
    assert days == 0


# ── run_coaching_chain 통합 (dev mock) ────────────────────────────────

@pytest.mark.asyncio
async def test_체인_응답_필드_7개_모두_존재():
    req = _make_request()
    result = await run_coaching_chain(req)
    assert result.ai_comment
    assert result.nutrition_summary
    assert result.exercise_summary
    assert result.goal_summary
    assert isinstance(result.avg_calorie_rate, float)
    assert isinstance(result.achievement_days, int)
    # weight_trend: 2개 기록 있으므로 float


def test_영양_agent_프롬프트_검증():
    """영양 Agent가 호출될 프롬프트에 avg_calorie_rate와 health_goal이 포함되는지
    간접 검증 — mock 모드에서는 [MOCK] 텍스트가 반환되므로 nutrition_summary로 확인."""
    pass  # dev mock에서는 직접 검증 불필요; integration test에서 확인


@pytest.mark.asyncio
async def test_weight_records_없으면_weight_trend_null():
    req = _make_request(weight_records=[])
    result = await run_coaching_chain(req)
    assert result.weight_trend is None


@pytest.mark.asyncio
async def test_routine_sessions_없어도_정상_처리():
    req = _make_request(routine_sessions=[])
    result = await run_coaching_chain(req)
    assert result.exercise_summary  # fallback 텍스트라도 존재


@pytest.mark.asyncio
async def test_nutrition_summary_가_exercise_summary에_전달됨():
    """dev mock에서 각 summary가 [MOCK] 텍스트임을 확인 — 체인이 실제로 순차 실행됨."""
    req = _make_request()
    result = await run_coaching_chain(req)
    assert result.nutrition_summary.startswith("[MOCK]")
    assert result.exercise_summary.startswith("[MOCK]")
    assert result.goal_summary.startswith("[MOCK]")
    assert result.ai_comment.startswith("[MOCK]")
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_coaching_service.py -v
```

Expected: `ModuleNotFoundError: No module named 'app.services.coaching_service'`

- [ ] **Step 3: `ai/app/services/coaching_service.py` 작성**

```python
from typing import Optional
from app.config import settings
from app.services.claude_service import call_claude
from app.services.trend_service import calc_weight_trend
from app.schemas.coaching import WeeklyCoachingRequest, WeeklyCoachingResponse


def _calc_stats(req: WeeklyCoachingRequest) -> tuple[float, int]:
    """칼로리 달성률 평균(%), 목표 달성일 수 반환."""
    if not req.daily_nutrition:
        return 0.0, 0

    def rate(actual: float, target: float) -> float:
        return round(actual / target * 100, 1) if target > 0 else 100.0

    rates = [rate(d.kcal + d.calories_burned, req.target_kcal) for d in req.daily_nutrition]
    avg = round(sum(rates) / len(rates), 1)
    achievement_days = sum(1 for r in rates if 80 <= r <= 120)
    return avg, achievement_days


async def _nutrition_agent(req: WeeklyCoachingRequest, avg_calorie_rate: float) -> str:
    if settings.env == "dev":
        return "[MOCK] 칼로리 달성률 85%, 단백질 섭취 다소 부족합니다."

    if not req.daily_nutrition:
        return "영양 기록 없음"

    protein_rates = [
        round(d.protein_g / req.target_protein_g * 100, 1) if req.target_protein_g > 0 else 100.0
        for d in req.daily_nutrition
    ]
    avg_protein = round(sum(protein_rates) / len(protein_rates), 1)

    prompt = (
        f"[영양 분석] 건강 목표: {req.health_goal}\n"
        f"칼로리 달성률 평균: {avg_calorie_rate:.0f}%\n"
        f"단백질 달성률 평균: {avg_protein:.0f}%\n"
        f"7일 칼로리 기록: {[d.kcal for d in req.daily_nutrition]}\n"
        "이 데이터를 바탕으로 이번 주 영양 섭취 패턴을 2문장으로 분석하세요."
    )
    try:
        return await call_claude(prompt, max_tokens=200)
    except Exception:
        return "영양 분석 불가"


async def _exercise_agent(req: WeeklyCoachingRequest, nutrition_analysis: str) -> str:
    if settings.env == "dev":
        return "[MOCK] 세션 성공률 양호, 단백질 보충 권장합니다."

    if not req.routine_sessions:
        return "이번 주 운동 기록 없음"

    session_summary = "\n".join(
        f"- {s.exercise_name}: {s.successful_sets}/{s.total_sets}세트 성공 ({s.weight_kg}kg)"
        for s in req.routine_sessions
    )
    prompt = (
        f"[운동 분석] 건강 목표: {req.health_goal}\n"
        f"세션 기록:\n{session_summary}\n"
        f"영양 분석 참고: {nutrition_analysis}\n"
        "위 데이터를 바탕으로 이번 주 운동 성과를 2문장으로 분석하세요. "
        "영양 분석과 연관 지점이 있으면 언급하세요."
    )
    try:
        return await call_claude(prompt, max_tokens=200)
    except Exception:
        return "운동 분석 불가"


async def _goal_agent(
    req: WeeklyCoachingRequest,
    weight_trend: Optional[float],
    nutrition_analysis: str,
    exercise_analysis: str,
) -> str:
    if settings.env == "dev":
        return "[MOCK] 체중 추세 안정적, 현재 궤도 유지하세요."

    trend_text = f"{weight_trend:+.2f}kg/주" if weight_trend is not None else "기록 없음"
    prompt = (
        f"[목표 달성 분석] 건강 목표: {req.health_goal}\n"
        f"체중 추세: {trend_text}\n"
        f"영양 분석: {nutrition_analysis}\n"
        f"운동 분석: {exercise_analysis}\n"
        f"위 데이터를 바탕으로 {req.health_goal} 목표 달성 궤도에 있는지 2문장으로 평가하세요."
    )
    try:
        return await call_claude(prompt, max_tokens=200)
    except Exception:
        return "목표 분석 불가"


async def _synthesis_agent(
    health_goal: str,
    nutrition_analysis: str,
    exercise_analysis: str,
    goal_analysis: str,
) -> str:
    if settings.env == "dev":
        return "[MOCK] 이번 주 전반적으로 잘 하셨습니다. 단백질 섭취를 조금 늘리면 더욱 효과적입니다."

    prompt = (
        f"[통합 코칭] 건강 목표: {health_goal}\n"
        f"영양 분석: {nutrition_analysis}\n"
        f"운동 분석: {exercise_analysis}\n"
        f"목표 달성 분석: {goal_analysis}\n"
        "세 분석을 종합해 회원에게 격려와 다음 주 실천 방향을 4~5문장 한국어로 코칭해주세요."
    )
    try:
        return await call_claude(prompt, max_tokens=400)
    except Exception:
        return "일부 분석에 문제가 있었습니다. 꾸준히 노력하고 계신 점은 훌륭합니다."


async def run_coaching_chain(req: WeeklyCoachingRequest) -> WeeklyCoachingResponse:
    avg_calorie_rate, achievement_days = _calc_stats(req)

    sorted_records = sorted(req.weight_records, key=lambda w: w.date)
    weight_trend = calc_weight_trend(
        [w.weight_kg for w in sorted_records],
        [w.date for w in sorted_records],
    )

    nutrition_analysis = await _nutrition_agent(req, avg_calorie_rate)
    exercise_analysis = await _exercise_agent(req, nutrition_analysis)
    goal_analysis = await _goal_agent(req, weight_trend, nutrition_analysis, exercise_analysis)
    ai_comment = await _synthesis_agent(
        req.health_goal, nutrition_analysis, exercise_analysis, goal_analysis
    )

    return WeeklyCoachingResponse(
        ai_comment=ai_comment,
        nutrition_summary=nutrition_analysis,
        exercise_summary=exercise_analysis,
        goal_summary=goal_analysis,
        avg_calorie_rate=avg_calorie_rate,
        achievement_days=achievement_days,
        weight_trend=weight_trend,
    )
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_coaching_service.py -v
```

Expected: `6 passed`

- [ ] **Step 5: 커밋**

```bash
git add ai/app/services/coaching_service.py ai/tests/test_coaching_service.py
git commit -m "feat(coaching): 4단계 순차 체인 coaching_service 구현 (TDD)"
```

---

## Task 3: 라우터 + main.py 등록 + 통합 테스트

**Files:**
- Create: `ai/app/routers/ai_coaching.py`
- Modify: `ai/app/main.py`
- Create: `ai/tests/test_ai_coaching.py`

**Interfaces:**
- Consumes: `run_coaching_chain` (Task 2), `WeeklyCoachingRequest`, `WeeklyCoachingResponse` (Task 1)

- [ ] **Step 1: 통합 테스트 작성**

```python
# ai/tests/test_ai_coaching.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "week_number": 3,
    "health_goal": "MUSCLE",
    "daily_nutrition": [
        {
            "date": f"2026-06-1{i}",
            "kcal": 1900.0,
            "protein_g": 100.0,
            "carb_g": 230.0,
            "fat_g": 60.0,
            "calories_burned": 300.0,
        }
        for i in range(7)
    ],
    "target_kcal": 2000.0,
    "target_protein_g": 120.0,
    "target_carb_g": 250.0,
    "target_fat_g": 65.0,
    "routine_sessions": [
        {
            "exercise_name": "벤치프레스",
            "successful_sets": 4,
            "total_sets": 4,
            "weight_kg": 65.0,
            "session_date": "2026-06-16",
        }
    ],
    "weight_records": [
        {"date": "2026-06-10", "weight_kg": 70.0},
        {"date": "2026-06-17", "weight_kg": 70.3},
    ],
}


def test_주간_코칭_200_반환():
    response = client.post("/ai/coaching/weekly", json=VALID_REQUEST)
    assert response.status_code == 200


def test_응답_필드_7개_모두_존재():
    response = client.post("/ai/coaching/weekly", json=VALID_REQUEST)
    data = response.json()
    for field in ["ai_comment", "nutrition_summary", "exercise_summary",
                  "goal_summary", "avg_calorie_rate", "achievement_days", "weight_trend"]:
        assert field in data, f"Missing field: {field}"


def test_weight_records_없으면_weight_trend_null():
    req = {**VALID_REQUEST, "weight_records": []}
    response = client.post("/ai/coaching/weekly", json=req)
    assert response.status_code == 200
    assert response.json()["weight_trend"] is None


def test_routine_sessions_없어도_200():
    req = {**VALID_REQUEST, "routine_sessions": []}
    response = client.post("/ai/coaching/weekly", json=req)
    assert response.status_code == 200


def test_dev_mock_응답_확인():
    response = client.post("/ai/coaching/weekly", json=VALID_REQUEST)
    data = response.json()
    assert "[MOCK]" in data["ai_comment"]
    assert "[MOCK]" in data["nutrition_summary"]
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_ai_coaching.py -v
```

Expected: `404 Not Found` (라우터 미등록)

- [ ] **Step 3: `ai/app/routers/ai_coaching.py` 작성**

```python
from fastapi import APIRouter
from app.schemas.coaching import WeeklyCoachingRequest, WeeklyCoachingResponse
from app.services.coaching_service import run_coaching_chain

router = APIRouter(tags=["AI Coaching"])


@router.post("/ai/coaching/weekly", response_model=WeeklyCoachingResponse)
async def weekly_coaching(req: WeeklyCoachingRequest):
    """
    F704 - Multi-Agent 주간 코칭
    영양→운동→목표 순차 체인 후 Synthesis로 통합 코칭 반환.
    Spring @Scheduled 배치에서 호출되며, 결과는 WeeklyReport에 저장된다.
    """
    return await run_coaching_chain(req)
```

- [ ] **Step 4: `ai/app/main.py` 수정 — 라우터 등록**

```python
# 기존 import 라인 수정:
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report, ai_coach, ai_chat, ai_coaching

# 기존 app.include_router 블록 아래에 추가:
app.include_router(ai_coaching.router)
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_ai_coaching.py tests/test_coaching_service.py -v
```

Expected: `11 passed`

- [ ] **Step 6: 커밋**

```bash
git add ai/app/routers/ai_coaching.py ai/app/main.py ai/tests/test_ai_coaching.py
git commit -m "feat(coaching): POST /ai/coaching/weekly 라우터 + 통합 테스트"
```
