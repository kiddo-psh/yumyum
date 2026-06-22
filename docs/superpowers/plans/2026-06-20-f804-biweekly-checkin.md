# F804 2주 체크인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /ai/checkin/biweekly` 엔드포인트를 구현해 2주 달성률 저조 시 체크인 타입·목표 조정 옵션·AI 격려 코멘트를 반환한다 (연동 #7).

**Architecture:** Spring이 2주 DailyGoal 달성률 < 50% 일 때 호출. FastAPI는 달성률을 LOW/VERY_LOW로 분류하고, 건강 목표(DIET/MUSCLE/HEALTH/DISEASE)에 따라 2~3개의 목표 조정 옵션을 생성한 뒤 AI 격려 코멘트를 덧붙여 반환. 순수 계산 로직은 `checkin_service.py`에 분리해 TDD로 검증.

**Tech Stack:** FastAPI, Pydantic v2, `call_claude` (GMS proxy), pytest

---

## 파일 구조

| 역할 | 파일 | 변경 |
|------|------|------|
| Pydantic 스키마 | `ai/app/schemas/report.py` | 수정 (3개 클래스 추가) |
| 순수 계산 서비스 | `ai/app/services/checkin_service.py` | 신규 생성 |
| 라우터 엔드포인트 | `ai/app/routers/ai_report.py` | 수정 (엔드포인트 추가) |
| Mock 응답 | `ai/app/services/claude_service.py` | 수정 (분기 추가) |
| 서비스 단위 테스트 | `ai/tests/test_checkin_service.py` | 신규 생성 |
| 통합 테스트 | `ai/tests/test_ai_report.py` | 수정 (6개 테스트 추가) |

> `ai/app/main.py`는 변경 불필요 — `ai_report.router`가 이미 등록되어 있음.

---

## Task 1: Pydantic 스키마 추가

**Files:**
- Modify: `ai/app/schemas/report.py`

- [ ] **Step 1: 3개 클래스를 `report.py` 하단에 추가**

```python
# ai/app/schemas/report.py 전체 (기존 내용 + 아래 3개 클래스 추가)
from pydantic import BaseModel
from typing import List, Optional, Literal


class DailyNutrition(BaseModel):
    date: str
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float
    calories_burned: float = 0.0


class WeightRecord(BaseModel):
    date: str
    weight_kg: float


class WeeklyReportRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    daily_nutrition: List[DailyNutrition]
    weight_records: List[WeightRecord]


class WeeklyReportResponse(BaseModel):
    avg_calorie_rate: float
    avg_protein_rate: float
    avg_carb_rate: float
    avg_fat_rate: float
    achievement_days: int
    weight_trend: Optional[float]
    ai_comment: str


class WeeklyAdjustRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    sex: Literal["MALE", "FEMALE"]
    current_target_kcal: float
    weight_trend: Optional[float]


class WeeklyAdjustResponse(BaseModel):
    new_target_kcal: float
    adjustment: float
    reason: str
    ai_comment: str


# ── F804 2주 체크인 ──────────────────────────────────────────────────

class AdjustmentOption(BaseModel):
    option_id: Literal["KEEP", "RELAX", "CHANGE_GOAL"]
    label: str
    description: str
    new_target_kcal: Optional[float] = None


class BiweeklyCheckinRequest(BaseModel):
    program_id: int
    week_number: int
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    sex: Literal["MALE", "FEMALE"]
    achievement_rate: float        # 0.0~100.0 (2주 DailyGoal 달성 일수 / 14 × 100)
    current_target_kcal: float


class BiweeklyCheckinResponse(BaseModel):
    checkin_type: Literal["LOW", "VERY_LOW"]
    adjustment_options: List[AdjustmentOption]
    ai_comment: str
```

- [ ] **Step 2: 임포트 확인 (파이썬 문법 오류 없음)**

```bash
cd ai && python -c "from app.schemas.report import BiweeklyCheckinRequest, BiweeklyCheckinResponse, AdjustmentOption; print('OK')"
```

Expected: `OK`

- [ ] **Step 3: 커밋**

```bash
git add ai/app/schemas/report.py
git commit -m "feat(checkin): F804 BiweeklyCheckin Pydantic 스키마 추가"
```

---

## Task 2: checkin_service TDD

**Files:**
- Create: `ai/tests/test_checkin_service.py`
- Create: `ai/app/services/checkin_service.py`

- [ ] **Step 1: 실패하는 테스트 작성**

```python
# ai/tests/test_checkin_service.py
import pytest
from app.services.checkin_service import classify_checkin, calc_adjustment_options


def test_달성률_30미만이면_VERY_LOW():
    assert classify_checkin(20.0) == "VERY_LOW"
    assert classify_checkin(0.0)  == "VERY_LOW"
    assert classify_checkin(29.9) == "VERY_LOW"


def test_달성률_30이상이면_LOW():
    assert classify_checkin(30.0) == "LOW"
    assert classify_checkin(49.9) == "LOW"


def test_DIET_옵션_3개_반환():
    options = calc_adjustment_options("DIET", 1800.0, "FEMALE")
    assert len(options) == 3


def test_MUSCLE_옵션_3개_반환():
    options = calc_adjustment_options("MUSCLE", 2200.0, "MALE")
    assert len(options) == 3


def test_HEALTH_옵션_2개_반환():
    options = calc_adjustment_options("HEALTH", 2000.0, "FEMALE")
    assert len(options) == 2


def test_DISEASE_옵션_2개_반환():
    options = calc_adjustment_options("DISEASE", 2000.0, "FEMALE")
    assert len(options) == 2


def test_KEEP_옵션이_항상_첫번째이고_칼로리_유지():
    for goal in ["DIET", "MUSCLE", "HEALTH", "DISEASE"]:
        options = calc_adjustment_options(goal, 2000.0, "FEMALE")
        assert options[0].option_id == "KEEP"
        assert options[0].new_target_kcal == 2000.0


def test_DIET_RELAX_옵션은_칼로리_100_증량():
    options = calc_adjustment_options("DIET", 1800.0, "FEMALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal == 1900.0


def test_MUSCLE_RELAX_옵션은_칼로리_100_감량():
    options = calc_adjustment_options("MUSCLE", 2200.0, "FEMALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal == 2100.0


def test_MUSCLE_RELAX_여성_하한선_1200_보장():
    options = calc_adjustment_options("MUSCLE", 1250.0, "FEMALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal >= 1200.0


def test_MUSCLE_RELAX_남성_하한선_1500_보장():
    options = calc_adjustment_options("MUSCLE", 1550.0, "MALE")
    relax = next(o for o in options if o.option_id == "RELAX")
    assert relax.new_target_kcal >= 1500.0


def test_CHANGE_GOAL_옵션은_new_target_kcal이_None():
    options = calc_adjustment_options("DIET", 1800.0, "FEMALE")
    change = next(o for o in options if o.option_id == "CHANGE_GOAL")
    assert change.new_target_kcal is None


def test_유효하지_않은_health_goal_ValueError():
    with pytest.raises(ValueError):
        calc_adjustment_options("UNKNOWN", 2000.0, "FEMALE")
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd ai && python -m pytest tests/test_checkin_service.py -v
```

Expected: ERROR (ImportError — 모듈 없음)

- [ ] **Step 3: checkin_service.py 구현**

```python
# ai/app/services/checkin_service.py
from app.schemas.report import AdjustmentOption

_MIN_KCAL: dict[str, float] = {"MALE": 1500.0, "FEMALE": 1200.0}
_RELAX_STEP = 100.0
_VALID_GOALS = {"DIET", "MUSCLE", "HEALTH", "DISEASE"}


def classify_checkin(achievement_rate: float) -> str:
    """
    2주 달성률 → 체크인 타입.
    < 30% → VERY_LOW, 30~50% → LOW
    """
    if achievement_rate < 30.0:
        return "VERY_LOW"
    return "LOW"


def calc_adjustment_options(
    health_goal: str,
    current_target_kcal: float,
    sex: str,
) -> list[AdjustmentOption]:
    """
    건강 목표별 조정 옵션 반환.
    DIET/MUSCLE → [KEEP, RELAX, CHANGE_GOAL] (3개)
    HEALTH/DISEASE → [KEEP, CHANGE_GOAL] (2개)
    """
    if health_goal not in _VALID_GOALS:
        raise ValueError(f"지원하지 않는 health_goal: {health_goal!r}")

    min_kcal = _MIN_KCAL.get(sex, 1200.0)

    keep = AdjustmentOption(
        option_id="KEEP",
        label="목표 유지",
        description="현재 목표를 그대로 이어나갑니다. 다음 2주를 더욱 집중해서 도전해보세요!",
        new_target_kcal=current_target_kcal,
    )
    change_goal = AdjustmentOption(
        option_id="CHANGE_GOAL",
        label="목표 변경 검토",
        description="건강 목표 타입 변경을 고려해보세요. 지속 가능한 목표가 장기적으로 더 효과적입니다.",
        new_target_kcal=None,
    )

    if health_goal == "DIET":
        relaxed_kcal = round(current_target_kcal + _RELAX_STEP, 1)
        relax = AdjustmentOption(
            option_id="RELAX",
            label="목표 완화",
            description=f"칼로리 목표를 {relaxed_kcal:.0f}kcal로 높여 지속 가능한 다이어트를 이어가세요.",
            new_target_kcal=relaxed_kcal,
        )
        return [keep, relax, change_goal]

    if health_goal == "MUSCLE":
        relaxed_kcal = round(max(min_kcal, current_target_kcal - _RELAX_STEP), 1)
        relax = AdjustmentOption(
            option_id="RELAX",
            label="목표 완화",
            description=f"칼로리 목표를 {relaxed_kcal:.0f}kcal로 낮춰 부담 없이 꾸준히 진행해보세요.",
            new_target_kcal=relaxed_kcal,
        )
        return [keep, relax, change_goal]

    # HEALTH, DISEASE: 칼로리 방향 조정 불필요 → KEEP + CHANGE_GOAL
    return [keep, change_goal]
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd ai && python -m pytest tests/test_checkin_service.py -v
```

Expected: 13 passed

- [ ] **Step 5: 커밋**

```bash
git add ai/app/services/checkin_service.py ai/tests/test_checkin_service.py
git commit -m "feat(checkin): F804 checkin_service 달성률 분류 + 옵션 계산 (TDD)"
```

---

## Task 3: 라우터 엔드포인트 + Mock + 통합 테스트

**Files:**
- Modify: `ai/app/routers/ai_report.py`
- Modify: `ai/app/services/claude_service.py`
- Modify: `ai/tests/test_ai_report.py`

- [ ] **Step 1: 통합 테스트 먼저 작성 (test_ai_report.py 하단에 추가)**

기존 `ai/tests/test_ai_report.py` 파일 끝에 아래 코드를 추가한다.

```python
# ── /ai/checkin/biweekly ─────────────────────────────────────────────

BIWEEKLY_CHECKIN_REQUEST = {
    "program_id": 1,
    "week_number": 2,
    "health_goal": "DIET",
    "sex": "FEMALE",
    "achievement_rate": 35.7,   # 30~50% → LOW
    "current_target_kcal": 1800.0,
}


def test_2주_체크인_성공시_200과_구조_반환():
    response = client.post("/ai/checkin/biweekly", json=BIWEEKLY_CHECKIN_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "checkin_type" in data
    assert "adjustment_options" in data
    assert "ai_comment" in data
    assert isinstance(data["adjustment_options"], list)
    assert len(data["ai_comment"]) > 0


def test_달성률_30미만이면_VERY_LOW_타입():
    req = {**BIWEEKLY_CHECKIN_REQUEST, "achievement_rate": 20.0}
    response = client.post("/ai/checkin/biweekly", json=req)
    assert response.json()["checkin_type"] == "VERY_LOW"


def test_달성률_30이상이면_LOW_타입():
    req = {**BIWEEKLY_CHECKIN_REQUEST, "achievement_rate": 42.8}
    response = client.post("/ai/checkin/biweekly", json=req)
    assert response.json()["checkin_type"] == "LOW"


def test_DIET_목표시_옵션_3개_포함():
    response = client.post("/ai/checkin/biweekly", json=BIWEEKLY_CHECKIN_REQUEST)
    assert len(response.json()["adjustment_options"]) == 3


def test_HEALTH_목표시_옵션_2개_포함():
    req = {**BIWEEKLY_CHECKIN_REQUEST, "health_goal": "HEALTH"}
    response = client.post("/ai/checkin/biweekly", json=req)
    assert len(response.json()["adjustment_options"]) == 2


def test_KEEP_옵션이_항상_첫번째_포함():
    response = client.post("/ai/checkin/biweekly", json=BIWEEKLY_CHECKIN_REQUEST)
    options = response.json()["adjustment_options"]
    assert options[0]["option_id"] == "KEEP"
    assert options[0]["new_target_kcal"] == BIWEEKLY_CHECKIN_REQUEST["current_target_kcal"]
```

- [ ] **Step 2: 테스트 실패 확인 (엔드포인트 없으므로 404)**

```bash
cd ai && python -m pytest tests/test_ai_report.py::test_2주_체크인_성공시_200과_구조_반환 -v
```

Expected: FAIL (404 Not Found)

- [ ] **Step 3: ai_report.py 에 엔드포인트 추가**

기존 import 블록과 라우터 선언 아래에 추가한다. 아래는 파일 전체 내용이다:

```python
# ai/app/routers/ai_report.py
from fastapi import APIRouter
from app.schemas.report import (
    WeeklyReportRequest, WeeklyReportResponse,
    WeeklyAdjustRequest, WeeklyAdjustResponse,
    BiweeklyCheckinRequest, BiweeklyCheckinResponse,
)
from app.services.claude_service import call_claude
from app.services.trend_service import calc_weight_trend, calc_calorie_adjustment
from app.services.checkin_service import classify_checkin, calc_adjustment_options

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

    sorted_records = sorted(req.weight_records, key=lambda w: w.date)
    weights = [w.weight_kg for w in sorted_records]
    dates   = [w.date     for w in sorted_records]
    weight_trend = calc_weight_trend(weights, dates)

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
        req.current_target_kcal, req.health_goal, req.weight_trend, req.sex
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


@router.post("/ai/checkin/biweekly", response_model=BiweeklyCheckinResponse)
async def biweekly_checkin(req: BiweeklyCheckinRequest):
    """
    연동 #7 - 2주 달성률 저조 체크인
    Spring이 2주 DailyGoal 달성률 < 50% 일 때 호출.
    달성률을 LOW/VERY_LOW로 분류하고 목표 조정 옵션 + AI 격려 코멘트를 반환한다.
    """
    checkin_type = classify_checkin(req.achievement_rate)
    options = calc_adjustment_options(req.health_goal, req.current_target_kcal, req.sex)

    low_label = "매우 저조" if checkin_type == "VERY_LOW" else "저조"
    prompt = (
        f"[{req.week_number}주차 2주 체크인] 건강 목표: {req.health_goal}\n"
        f"2주 달성률: {req.achievement_rate:.1f}% ({low_label})\n"
        f"현재 목표 칼로리: {req.current_target_kcal:.0f}kcal\n"
        "달성률이 낮은 사용자를 격려하고 실천 가능한 조언을 2~3문장으로 한국어로 제안하세요."
    )

    ai_comment = await call_claude(prompt, max_tokens=300)

    return BiweeklyCheckinResponse(
        checkin_type=checkin_type,
        adjustment_options=options,
        ai_comment=ai_comment,
    )
```

- [ ] **Step 4: claude_service.py mock 분기 추가**

`_mock_response` 함수 최상단(첫 번째 `if` 앞)에 체크인 분기를 추가한다.

```python
def _mock_response(prompt: str) -> str:
    """개발용 mock 응답 (크레딧 절약). JSON 요청 여부에 따라 형식 분리."""
    if "체크인" in prompt:
        return "2주 동안 꾸준히 노력하셨어요! 달성률이 낮더라도 포기하지 마세요. 목표를 조금 조정하면 더 오래 지속 가능한 건강 습관을 만들 수 있습니다."

    if "칼로리 조정" in prompt:
        return "체중 변화 추세를 분석해 목표 칼로리를 조정했습니다. 새로운 목표에 맞춰 식단을 구성해보세요!"

    if "리포트" in prompt or "주차" in prompt:
        return "이번 주도 꾸준히 노력하셨네요! 칼로리 달성률이 안정적으로 유지되고 있어요. 다음 주에는 단백질 섭취를 조금 더 신경 써보세요."

    # ... 이하 기존 분기 유지
```

- [ ] **Step 5: 전체 테스트 통과 확인**

```bash
cd ai && python -m pytest tests/ --ignore=tests/test_gms_api.py --ignore=tests/test_mfds_api.py -v
```

Expected: 86 passed (기존 68 + 신규 18)

- [ ] **Step 6: 커밋**

```bash
git add ai/app/routers/ai_report.py ai/app/services/claude_service.py ai/tests/test_ai_report.py
git commit -m "feat(checkin): F804 POST /ai/checkin/biweekly 구현 (연동 #7)"
```
