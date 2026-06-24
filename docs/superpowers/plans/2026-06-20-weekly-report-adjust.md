# F304 Weekly Report + Plan Adjust Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring이 7일 누적 데이터를 전송하면 numpy 추세 분석 + AI 피드백으로 주간 리포트를 생성하고, 체중 추세를 기반으로 다음 주 목표 칼로리를 재조정한다.

**Architecture:** `trend_service.py`(순수 함수 numpy 계산) → `ai_report.py` 라우터 두 엔드포인트. `/ai/report/weekly`가 체중 추세를 반환하면 Spring이 이를 저장 후 `/ai/plan/weekly-adjust`에 전달한다. 연동 포인트 #5, #6 완성.

**Tech Stack:** Python 3.11, FastAPI, numpy, pytest, httpx (TestClient)

---

## 파일 맵

### 신규
| 경로 | 역할 |
|---|---|
| `ai/app/schemas/report.py` | WeeklyReportRequest/Response, WeeklyAdjustRequest/Response Pydantic 모델 |
| `ai/app/services/trend_service.py` | numpy 선형 회귀로 체중 추세·칼로리 조정량 계산 (순수 함수) |
| `ai/app/routers/ai_report.py` | POST /ai/report/weekly, POST /ai/plan/weekly-adjust |
| `ai/tests/test_trend_service.py` | trend_service 단위 테스트 |
| `ai/tests/test_ai_report.py` | 엔드포인트 통합 테스트 |

### 수정
| 경로 | 변경 내용 |
|---|---|
| `ai/app/main.py` | ai_report 라우터 등록 |
| `ai/app/services/claude_service.py` | weekly-report/adjust mock 응답 분기 추가 |

---

## Spring ↔ FastAPI 계약 (참고용)

```
연동 #5: Program 7일 경과 시
  Spring → POST /ai/report/weekly
  Spring가 집계할 데이터: 7일 식단 합산, 체중 기록, Program 목표치

연동 #6: WeeklyReport 저장 후
  Spring → POST /ai/plan/weekly-adjust
  Spring가 전달할 데이터: 현재 목표 칼로리, HealthGoal, weight_trend (연동 #5 응답에서 획득)
```

---

## Task 1: trend_service.py — TDD

**Files:**
- Create: `ai/app/services/trend_service.py`
- Create: `ai/tests/test_trend_service.py`

- [ ] **Step 1: 테스트 파일 작성**

```python
# ai/tests/test_trend_service.py
import pytest
from app.services.trend_service import calc_weight_trend, calc_calorie_adjustment


# ── calc_weight_trend ────────────────────────────────────────────────

def test_체중이_일정하면_추세_0():
    result = calc_weight_trend([70.0, 70.0, 70.0, 70.0])
    assert result == pytest.approx(0.0, abs=0.01)


def test_체중이_꾸준히_감소하면_음수_반환():
    # 7일간 1kg 감소 → slope ≈ -1/7 per day → *7 ≈ -1.0 kg/week
    weights = [71.0, 70.857, 70.714, 70.571, 70.428, 70.285, 70.0]
    result = calc_weight_trend(weights)
    assert result == pytest.approx(-1.0, abs=0.05)


def test_체중이_꾸준히_증가하면_양수_반환():
    weights = [70.0, 70.1, 70.2, 70.3, 70.4, 70.5, 70.6]
    result = calc_weight_trend(weights)
    assert result is not None
    assert result > 0


def test_데이터_1개면_None_반환():
    assert calc_weight_trend([70.0]) is None


def test_빈_리스트면_None_반환():
    assert calc_weight_trend([]) is None


# ── calc_calorie_adjustment ──────────────────────────────────────────

def test_DIET_감소_과다시_칼로리_증량():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", -1.5)
    assert new_kcal == 1900.0
    assert "증량" in reason


def test_DIET_감소_적정시_유지():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", -0.5)
    assert new_kcal == 1800.0
    assert "유지" in reason


def test_DIET_감소_부족시_칼로리_감량():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", 0.1)
    assert new_kcal == 1700.0
    assert "감량" in reason


def test_MUSCLE_증가_과다시_칼로리_감량():
    new_kcal, reason = calc_calorie_adjustment(2200.0, "MUSCLE", 0.8)
    assert new_kcal == 2100.0
    assert "감량" in reason


def test_MUSCLE_증가_적정시_유지():
    new_kcal, reason = calc_calorie_adjustment(2200.0, "MUSCLE", 0.3)
    assert new_kcal == 2200.0
    assert "유지" in reason


def test_MUSCLE_증가_부족시_칼로리_증량():
    new_kcal, reason = calc_calorie_adjustment(2200.0, "MUSCLE", 0.0)
    assert new_kcal == 2300.0
    assert "증량" in reason


def test_HEALTH_체중_안정시_유지():
    new_kcal, reason = calc_calorie_adjustment(2000.0, "HEALTH", 0.2)
    assert new_kcal == 2000.0
    assert "유지" in reason


def test_HEALTH_체중_변동_과다시_조정():
    new_kcal, reason = calc_calorie_adjustment(2000.0, "HEALTH", 0.8)
    assert new_kcal == 1900.0


def test_추세_None이면_현재_칼로리_유지():
    new_kcal, reason = calc_calorie_adjustment(1800.0, "DIET", None)
    assert new_kcal == 1800.0
    assert "부족" in reason


def test_칼로리_하한선_1200_미만으로_내려가지_않음():
    new_kcal, _ = calc_calorie_adjustment(1250.0, "DIET", 0.5)
    assert new_kcal >= 1200.0
```

- [ ] **Step 2: 실패 확인**

```powershell
cd C:\kiddo\projects\pjt\yumyum\ai
.\venv\Scripts\python -m pytest tests/test_trend_service.py -v
```
Expected: `ImportError` 또는 `ModuleNotFoundError`

- [ ] **Step 3: trend_service.py 구현**

```python
# ai/app/services/trend_service.py
import numpy as np
from typing import List, Optional


def calc_weight_trend(weights: List[float]) -> Optional[float]:
    """
    선형 회귀로 주간 체중 변화량(kg/week) 계산.

    Args:
        weights: 날짜 오름차순으로 정렬된 체중 목록 (최소 2개 필요)
    Returns:
        kg/week 변화량 (양수=증가, 음수=감소), 데이터 2개 미만이면 None
    """
    if len(weights) < 2:
        return None
    x = np.arange(len(weights), dtype=float)
    slope, _ = np.polyfit(x, weights, 1)
    return round(float(slope) * 7, 3)


_ADJUSTMENT_STEP = 100.0
_MIN_KCAL = 1200.0


def calc_calorie_adjustment(
    current_kcal: float,
    health_goal: str,
    weight_trend: Optional[float],
) -> tuple[float, str]:
    """
    체중 추세와 건강 목표를 기반으로 목표 칼로리 조정량 계산.

    Args:
        current_kcal: 현재 목표 칼로리
        health_goal: "DIET" | "MUSCLE" | "HEALTH" | "DISEASE"
        weight_trend: kg/week 변화량. None이면 데이터 부족으로 유지
    Returns:
        (new_kcal, reason)
    """
    if weight_trend is None:
        return current_kcal, "체중 데이터 부족으로 유지"

    adjustment = 0.0
    reason = ""

    if health_goal == "DIET":
        if weight_trend < -1.0:
            adjustment = +_ADJUSTMENT_STEP
            reason = f"체중 감소 속도 과다({weight_trend:+.2f}kg/주) → 칼로리 증량"
        elif weight_trend > -0.25:
            adjustment = -_ADJUSTMENT_STEP
            reason = f"감소 속도 부족({weight_trend:+.2f}kg/주) → 칼로리 감량"
        else:
            reason = f"적정 감소 속도({weight_trend:+.2f}kg/주) → 유지"
    elif health_goal == "MUSCLE":
        if weight_trend > 0.5:
            adjustment = -_ADJUSTMENT_STEP
            reason = f"체중 증가 속도 과다({weight_trend:+.2f}kg/주) → 칼로리 감량"
        elif weight_trend < 0.1:
            adjustment = +_ADJUSTMENT_STEP
            reason = f"증가 속도 부족({weight_trend:+.2f}kg/주) → 칼로리 증량"
        else:
            reason = f"적정 증가 속도({weight_trend:+.2f}kg/주) → 유지"
    else:  # HEALTH, DISEASE
        if abs(weight_trend) > 0.5:
            adjustment = -_ADJUSTMENT_STEP if weight_trend > 0 else +_ADJUSTMENT_STEP
            reason = f"체중 변동 과다({weight_trend:+.2f}kg/주) → 칼로리 조정"
        else:
            reason = f"체중 안정({weight_trend:+.2f}kg/주) → 유지"

    new_kcal = max(_MIN_KCAL, current_kcal + adjustment)
    return round(new_kcal, 1), reason
```

- [ ] **Step 4: 테스트 통과 확인**

```powershell
cd C:\kiddo\projects\pjt\yumyum\ai
.\venv\Scripts\python -m pytest tests/test_trend_service.py -v
```
Expected: 모든 테스트 PASS

- [ ] **Step 5: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add ai/app/services/trend_service.py ai/tests/test_trend_service.py
git commit -m "feat(report): trend_service 체중 추세·칼로리 조정 로직 (TDD)"
```

---

## Task 2: report.py 스키마

**Files:**
- Create: `ai/app/schemas/report.py`

- [ ] **Step 1: 스키마 작성**

```python
# ai/app/schemas/report.py
from pydantic import BaseModel
from typing import List, Optional


class DailyNutrition(BaseModel):
    date: str           # "2026-06-13"
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    calories_burned: float = 0.0   # 운동 소모 칼로리


class WeightRecord(BaseModel):
    date: str           # "2026-06-13"
    weight_kg: float


class WeeklyReportRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: str              # DIET | MUSCLE | HEALTH | DISEASE
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    daily_nutrition: List[DailyNutrition]   # 최대 7일
    weight_records: List[WeightRecord]       # 체중 기록 (없을 수 있음)


class WeeklyReportResponse(BaseModel):
    avg_calorie_rate: float      # 7일 평균 칼로리 달성률 %
    avg_protein_rate: float
    avg_carb_rate: float
    avg_fat_rate: float
    achievement_days: int        # 칼로리 달성률 80~120% 내에 든 날 수
    weight_trend: Optional[float]   # kg/week 변화량. 기록 없으면 None
    ai_comment: str


class WeeklyAdjustRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: str              # DIET | MUSCLE | HEALTH | DISEASE
    current_target_kcal: float
    weight_trend: Optional[float]   # kg/week. /ai/report/weekly 응답에서 전달


class WeeklyAdjustResponse(BaseModel):
    new_target_kcal: float
    adjustment: float             # 변화량 (+/- kcal)
    reason: str
    ai_comment: str
```

- [ ] **Step 2: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add ai/app/schemas/report.py
git commit -m "feat(report): WeeklyReport/Adjust Pydantic 스키마 추가"
```

---

## Task 3: ai_report.py 라우터 — POST /ai/report/weekly

**Files:**
- Create: `ai/app/routers/ai_report.py`

- [ ] **Step 1: 라우터 구현**

```python
# ai/app/routers/ai_report.py
from fastapi import APIRouter
from app.schemas.report import (
    WeeklyReportRequest, WeeklyReportResponse,
    WeeklyAdjustRequest, WeeklyAdjustResponse,
)
from app.services.claude_service import call_claude
from app.services.trend_service import calc_weight_trend, calc_calorie_adjustment

router = APIRouter(tags=["AI Report"])


@router.post("/ai/report/weekly", response_model=WeeklyReportResponse)
async def weekly_report(req: WeeklyReportRequest):
    """
    F304 - 주간 리포트 생성
    7일 식단·운동 데이터를 분석해 달성률과 체중 추세를 계산하고 AI 피드백을 반환한다.
    """
    def rate(actual: float, target: float) -> float:
        return round(actual / target * 100, 1) if target > 0 else 100.0

    calorie_rates = [
        rate(d.kcal + d.calories_burned, req.target_kcal)
        for d in req.daily_nutrition
    ]
    protein_rates  = [rate(d.protein_g, req.target_protein_g) for d in req.daily_nutrition]
    carb_rates     = [rate(d.carb_g,    req.target_carb_g)    for d in req.daily_nutrition]
    fat_rates      = [rate(d.fat_g,     req.target_fat_g)     for d in req.daily_nutrition]

    avg_cal  = round(sum(calorie_rates) / len(calorie_rates), 1) if calorie_rates else 0.0
    avg_pro  = round(sum(protein_rates) / len(protein_rates), 1) if protein_rates else 0.0
    avg_carb = round(sum(carb_rates)    / len(carb_rates),    1) if carb_rates    else 0.0
    avg_fat  = round(sum(fat_rates)     / len(fat_rates),     1) if fat_rates     else 0.0

    achievement_days = sum(1 for r in calorie_rates if 80 <= r <= 120)

    weights = [w.weight_kg for w in req.weight_records]
    weight_trend = calc_weight_trend(weights)

    trend_text = (
        f"체중 추세: {weight_trend:+.2f}kg/주"
        if weight_trend is not None
        else "이번 주 체중 기록 없음"
    )
    prompt = (
        f"[{req.week_number}주차 리포트] 건강 목표: {req.health_goal}\n"
        f"칼로리 달성률 평균: {avg_cal:.0f}% | 단백질: {avg_pro:.0f}% | "
        f"탄수화물: {avg_carb:.0f}% | 지방: {avg_fat:.0f}%\n"
        f"목표 달성일: {achievement_days}일 / {len(req.daily_nutrition)}일\n"
        f"{trend_text}\n"
        "위 결과를 바탕으로 이번 주를 격려하고 다음 주 개선 방향을 2~3문장으로 한국어로 제안하세요."
    )

    ai_comment = await call_claude(prompt, max_tokens=400)

    return WeeklyReportResponse(
        avg_calorie_rate=avg_cal,
        avg_protein_rate=avg_pro,
        avg_carb_rate=avg_carb,
        avg_fat_rate=avg_fat,
        achievement_days=achievement_days,
        weight_trend=weight_trend,
        ai_comment=ai_comment,
    )


@router.post("/ai/plan/weekly-adjust", response_model=WeeklyAdjustResponse)
async def weekly_adjust(req: WeeklyAdjustRequest):
    """
    연동 #6 - 주간 체중 추세 기반 목표 칼로리 재조정
    /ai/report/weekly 응답의 weight_trend를 받아 다음 주 칼로리 목표를 조정한다.
    """
    new_kcal, reason = calc_calorie_adjustment(
        req.current_target_kcal, req.health_goal, req.weight_trend
    )
    adjustment = new_kcal - req.current_target_kcal

    prompt = (
        f"[{req.week_number}주차 칼로리 조정] 목표: {req.health_goal}\n"
        f"기존: {req.current_target_kcal:.0f}kcal → 조정: {new_kcal:.0f}kcal ({adjustment:+.0f})\n"
        f"이유: {reason}\n"
        "이 조정 내용을 사용자에게 이해하기 쉽게 2문장으로 한국어로 설명하세요."
    )

    ai_comment = await call_claude(prompt, max_tokens=200)

    return WeeklyAdjustResponse(
        new_target_kcal=new_kcal,
        adjustment=adjustment,
        reason=reason,
        ai_comment=ai_comment,
    )
```

- [ ] **Step 2: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add ai/app/routers/ai_report.py
git commit -m "feat(report): POST /ai/report/weekly + /ai/plan/weekly-adjust 구현"
```

---

## Task 4: mock 응답 추가 + 라우터 등록

**Files:**
- Modify: `ai/app/services/claude_service.py`
- Modify: `ai/app/main.py`

- [ ] **Step 1: claude_service.py mock 분기 추가**

`_mock_response` 함수 맨 앞에 아래 두 분기를 추가한다 (기존 코드 앞에 삽입):

```python
    if "리포트" in prompt or "주차" in prompt:
        return "이번 주도 꾸준히 노력하셨네요! 칼로리 달성률이 안정적으로 유지되고 있어요. 다음 주에는 단백질 섭취를 조금 더 신경 써보세요."

    if "칼로리 조정" in prompt:
        return "체중 변화 추세를 분석해 목표 칼로리를 조정했습니다. 새로운 목표에 맞춰 식단을 구성해보세요!"
```

- [ ] **Step 2: main.py 라우터 등록**

```python
# ai/app/main.py — 수정 후 전체
from fastapi import FastAPI
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report

app = FastAPI(
    title="냠냠코치 AI Server",
    description="Spring Boot ↔ FastAPI AI 연동 서버",
    version="1.0.0",
)

app.include_router(ai_meal.router)
app.include_router(ai_plan.router)
app.include_router(ai_routine.router)
app.include_router(food.router)
app.include_router(ai_report.router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "nyamnyam-fastapi"}
```

- [ ] **Step 3: 서버 기동 확인**

```powershell
cd C:\kiddo\projects\pjt\yumyum\ai
.\venv\Scripts\uvicorn app.main:app --reload --port 8000
```
Expected: `Application startup complete.` 오류 없음. Ctrl+C로 종료.

- [ ] **Step 4: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add ai/app/services/claude_service.py ai/app/main.py
git commit -m "feat(report): mock 응답 추가 및 ai_report 라우터 등록"
```

---

## Task 5: 통합 테스트

**Files:**
- Create: `ai/tests/test_ai_report.py`

- [ ] **Step 1: 테스트 작성**

```python
# ai/tests/test_ai_report.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

DAILY_NUTRITION = [
    {"date": f"2026-06-{13+i:02d}", "kcal": 1800.0 + i * 20, "protein_g": 130.0,
     "carb_g": 220.0, "fat_g": 55.0, "calories_burned": 200.0}
    for i in range(7)
]
WEIGHT_RECORDS = [
    {"date": f"2026-06-{13+i:02d}", "weight_kg": 80.0 - i * 0.1}
    for i in range(7)
]
WEEKLY_REPORT_REQUEST = {
    "program_id": 1,
    "week_number": 1,
    "health_goal": "DIET",
    "target_kcal": 2000.0,
    "target_protein_g": 150.0,
    "target_carb_g": 240.0,
    "target_fat_g": 60.0,
    "daily_nutrition": DAILY_NUTRITION,
    "weight_records": WEIGHT_RECORDS,
}


def test_주간_리포트_성공시_200과_구조_반환():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "avg_calorie_rate" in data
    assert "avg_protein_rate" in data
    assert "avg_carb_rate" in data
    assert "avg_fat_rate" in data
    assert "achievement_days" in data
    assert "weight_trend" in data
    assert "ai_comment" in data
    assert isinstance(data["ai_comment"], str)
    assert len(data["ai_comment"]) > 0


def test_달성률_평균이_올바르게_계산된다():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    data = response.json()
    # kcal + burned / target 평균 검증
    # 첫날: (1800+200)/2000 = 100%, 마지막날: (1920+200)/2000 = 106%
    assert 95.0 <= data["avg_calorie_rate"] <= 115.0


def test_체중_기록_있으면_weight_trend_반환():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    data = response.json()
    assert data["weight_trend"] is not None
    assert data["weight_trend"] < 0   # 체중 감소 추세


def test_체중_기록_없으면_weight_trend_None():
    req = {**WEEKLY_REPORT_REQUEST, "weight_records": []}
    response = client.post("/ai/report/weekly", json=req)
    assert response.status_code == 200
    assert response.json()["weight_trend"] is None


def test_달성일수는_0이상_7이하():
    response = client.post("/ai/report/weekly", json=WEEKLY_REPORT_REQUEST)
    data = response.json()
    assert 0 <= data["achievement_days"] <= 7


# ── /ai/plan/weekly-adjust ───────────────────────────────────────────

WEEKLY_ADJUST_REQUEST = {
    "program_id": 1,
    "week_number": 1,
    "health_goal": "DIET",
    "current_target_kcal": 1800.0,
    "weight_trend": 0.2,  # 감소 안 됨 → 칼로리 감량 필요
}


def test_칼로리_조정_성공시_200과_구조_반환():
    response = client.post("/ai/plan/weekly-adjust", json=WEEKLY_ADJUST_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "new_target_kcal" in data
    assert "adjustment" in data
    assert "reason" in data
    assert "ai_comment" in data
    assert isinstance(data["reason"], str)
    assert len(data["ai_comment"]) > 0


def test_DIET_감소_부족시_칼로리_감량():
    response = client.post("/ai/plan/weekly-adjust", json=WEEKLY_ADJUST_REQUEST)
    data = response.json()
    assert data["new_target_kcal"] == 1700.0
    assert data["adjustment"] == -100.0


def test_칼로리_하한선_1200_보장():
    req = {**WEEKLY_ADJUST_REQUEST, "current_target_kcal": 1250.0}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    assert response.json()["new_target_kcal"] >= 1200.0


def test_weight_trend_None이면_칼로리_유지():
    req = {**WEEKLY_ADJUST_REQUEST, "weight_trend": None}
    response = client.post("/ai/plan/weekly-adjust", json=req)
    data = response.json()
    assert data["new_target_kcal"] == WEEKLY_ADJUST_REQUEST["current_target_kcal"]
    assert data["adjustment"] == 0.0
```

- [ ] **Step 2: 테스트 실행**

```powershell
cd C:\kiddo\projects\pjt\yumyum\ai
.\venv\Scripts\python -m pytest tests/test_ai_report.py -v
```
Expected: 모든 테스트 PASS

- [ ] **Step 3: 전체 테스트 회귀 확인**

```powershell
cd C:\kiddo\projects\pjt\yumyum\ai
.\venv\Scripts\python -m pytest tests/ -v --ignore=tests/test_gms_api.py --ignore=tests/test_mfds_api.py
```
Expected: 기존 테스트 포함 전체 PASS

- [ ] **Step 4: 커밋**

```powershell
cd C:\kiddo\projects\pjt\yumyum
git add ai/tests/test_ai_report.py
git commit -m "test(report): weekly-report/adjust 통합 테스트 추가"
```
