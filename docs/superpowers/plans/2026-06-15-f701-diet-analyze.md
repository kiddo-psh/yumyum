# F701 식단 분석 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot에서 오늘의 식단 데이터를 받아 영양 균형을 분석하고 Claude AI 코멘트를 포함한 결과를 반환하는 `POST /ai/meal/diet-analyze` 엔드포인트를 구현한다.

**Architecture:** 순수 계산 로직(`diet_service.py`)과 AI 호출(`claude_service.py`)을 분리. 엔드포인트는 `ai_meal.py`에 추가. 기존 `ai/app/schemas/meal.py`에 Request/Response 스키마를 추가한다.

**Tech Stack:** Python 3.11, FastAPI, Pydantic v2, pytest, httpx (claude_service 경유 GMS API)

---

## 파일 맵

| 파일 | 작업 | 역할 |
|---|---|---|
| `ai/app/schemas/meal.py` | 수정 | `DietAnalyzeRequest`, `DietAnalyzeResponse` 스키마 추가 |
| `ai/app/services/diet_service.py` | **신규** | 영양소 달성률·균형 점수·부족/과다 판단 순수 계산 |
| `ai/app/services/claude_service.py` | 수정 | `_mock_response`에 "분석" 키워드 분기 추가 |
| `ai/app/routers/ai_meal.py` | 수정 | `POST /ai/meal/diet-analyze` 엔드포인트 추가 |
| `ai/tests/test_diet_service.py` | **신규** | `diet_service` 단위 테스트 |
| `ai/tests/test_ai_meal_analyze.py` | **신규** | `/ai/meal/diet-analyze` E2E 통합 테스트 |

---

## Task 1: 스키마 추가

**Files:**
- Modify: `ai/app/schemas/meal.py`

- [ ] **Step 1: `DietAnalyzeRequest`, `DietAnalyzeResponse` 추가**

기존 파일 하단에 아래를 추가한다. 기존 클래스는 건드리지 않는다.

```python
# ai/app/schemas/meal.py (기존 내용 유지, 아래 추가)

class NutrientRate(BaseModel):
    calorie_rate: float   # 달성률 % (actual/target * 100)
    protein_rate: float
    carb_rate: float
    fat_rate: float


class DietAnalyzeRequest(BaseModel):
    total_kcal: float
    total_protein_g: float
    total_carb_g: float
    total_fat_g: float
    target_kcal: float
    target_protein_g: float
    target_carb_g: float
    target_fat_g: float
    health_goal: str   # DIET | MUSCLE | HEALTH | DISEASE
    meal_date: str     # "2026-06-15" (로깅·코멘트 생성용)


class DietAnalyzeResponse(BaseModel):
    calorie_rate: float          # 칼로리 달성률 %
    protein_rate: float          # 단백질 달성률 %
    carb_rate: float             # 탄수화물 달성률 %
    fat_rate: float              # 지방 달성률 %
    balance_score: float         # 균형 점수 0~100 (100에 가까울수록 균형)
    weak_nutrients: List[str]    # 달성률 80% 미만 영양소 목록
    excess_nutrients: List[str]  # 달성률 120% 초과 영양소 목록
    ai_comment: str
```

- [ ] **Step 2: import 확인**

```bash
cd ai && python -c "from app.schemas.meal import DietAnalyzeRequest, DietAnalyzeResponse; print('OK')"
```

Expected: `OK`

- [ ] **Step 3: 커밋**

```bash
cd ai
git add app/schemas/meal.py
git commit -m "feat(meal): F701 DietAnalyzeRequest/Response 스키마 추가"
```

---

## Task 2: diet_service.py TDD

**Files:**
- Create: `ai/app/services/diet_service.py`
- Create: `ai/tests/test_diet_service.py`

- [ ] **Step 1: 테스트 작성**

```python
# ai/tests/test_diet_service.py
from app.services.diet_service import calculate_diet_analysis


def test_목표치_정확히_달성하면_균형점수_100():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert rates["calorie_rate"] == 100.0
    assert rates["protein_rate"] == 100.0
    assert score == 100.0
    assert weak == []
    assert excess == []


def test_단백질_부족하면_weak_nutrients에_포함():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=100, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, total_carb_g=250, target_fat_g=60,
    )
    assert "protein" in weak
    assert rates["protein_rate"] < 80.0


def test_단백질_부족하면_weak_nutrients에_포함_v2():
    # total_protein = 100, target = 150 → rate = 66.7% < 80%
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=100, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert "protein" in weak
    assert rates["protein_rate"] == pytest.approx(66.67, abs=0.1)


def test_지방_과다하면_excess_nutrients에_포함():
    # total_fat = 80, target = 60 → rate = 133% > 120%
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=80,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert "fat" in excess
    assert rates["fat_rate"] == pytest.approx(133.33, abs=0.1)


def test_target이_0이면_달성률_100으로_처리():
    # 목표 지방이 0인 경우 — 0으로 나누기 방지
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=0,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=0,
    )
    assert rates["fat_rate"] == 100.0
    assert "fat" not in weak
    assert "fat" not in excess


def test_균형점수는_편차가_클수록_낮아진다():
    # 단백질만 절반 달성 → 편차 발생 → 점수 < 100
    rates_balanced, score_balanced, _, _ = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    rates_unbalanced, score_unbalanced, _, _ = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=75, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert score_balanced > score_unbalanced
```

먼저 import 줄을 파일 상단에 추가한다:

```python
# ai/tests/test_diet_service.py (파일 전체)
import pytest
from app.services.diet_service import calculate_diet_analysis


def test_목표치_정확히_달성하면_균형점수_100():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert rates["calorie_rate"] == 100.0
    assert rates["protein_rate"] == 100.0
    assert score == 100.0
    assert weak == []
    assert excess == []


def test_단백질_부족하면_weak_nutrients에_포함():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=100, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert "protein" in weak
    assert rates["protein_rate"] == pytest.approx(66.67, abs=0.1)


def test_지방_과다하면_excess_nutrients에_포함():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=80,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert "fat" in excess
    assert rates["fat_rate"] == pytest.approx(133.33, abs=0.1)


def test_target이_0이면_달성률_100으로_처리():
    rates, score, weak, excess = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=0,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=0,
    )
    assert rates["fat_rate"] == 100.0
    assert "fat" not in weak
    assert "fat" not in excess


def test_균형점수는_편차가_클수록_낮아진다():
    _, score_balanced, _, _ = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=150, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    _, score_unbalanced, _, _ = calculate_diet_analysis(
        total_kcal=2000, total_protein_g=75, total_carb_g=250, total_fat_g=60,
        target_kcal=2000, target_protein_g=150, target_carb_g=250, target_fat_g=60,
    )
    assert score_balanced > score_unbalanced
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_diet_service.py -v
```

Expected: `ImportError: cannot import name 'calculate_diet_analysis'`

- [ ] **Step 3: diet_service.py 구현**

```python
# ai/app/services/diet_service.py
from typing import Dict, List, Tuple


def calculate_diet_analysis(
    total_kcal: float, total_protein_g: float,
    total_carb_g: float, total_fat_g: float,
    target_kcal: float, target_protein_g: float,
    target_carb_g: float, target_fat_g: float,
) -> Tuple[Dict[str, float], float, List[str], List[str]]:
    """
    Returns:
        rates: {"calorie_rate": float, "protein_rate": float, "carb_rate": float, "fat_rate": float}
        balance_score: 0~100 (100 = 완벽한 균형)
        weak_nutrients: 달성률 80% 미만 항목 리스트
        excess_nutrients: 달성률 120% 초과 항목 리스트
    """
    def rate(actual: float, target: float) -> float:
        if target <= 0:
            return 100.0
        return round(actual / target * 100, 2)

    rates = {
        "calorie_rate": rate(total_kcal, target_kcal),
        "protein_rate": rate(total_protein_g, target_protein_g),
        "carb_rate":    rate(total_carb_g, target_carb_g),
        "fat_rate":     rate(total_fat_g, target_fat_g),
    }

    values = list(rates.values())
    mean_deviation = sum(abs(r - 100) for r in values) / len(values)
    balance_score = round(max(0.0, 100.0 - mean_deviation), 2)

    label_map = {
        "calorie_rate": "calorie",
        "protein_rate": "protein",
        "carb_rate":    "carb",
        "fat_rate":     "fat",
    }
    weak   = [label_map[k] for k, v in rates.items() if v < 80]
    excess = [label_map[k] for k, v in rates.items() if v > 120]

    return rates, balance_score, weak, excess
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_diet_service.py -v
```

Expected: `5 passed`

- [ ] **Step 5: 커밋**

```bash
cd ai
git add app/services/diet_service.py tests/test_diet_service.py
git commit -m "feat(meal): F701 diet_service 영양소 균형 계산 로직 (TDD)"
```

---

## Task 3: claude_service mock 분기 추가

**Files:**
- Modify: `ai/app/services/claude_service.py`

- [ ] **Step 1: `_mock_response`에 "분석" 분기 추가**

기존 `_mock_response` 함수의 첫 번째 `if` 블록 위에 아래 분기를 추가한다:

```python
def _mock_response(prompt: str) -> str:
    """개발용 mock 응답 (크레딧 절약). JSON 요청 여부에 따라 형식 분리."""
    # ── 신규 추가 ──────────────────────────────────────────
    if "영양 균형" in prompt or "diet" in prompt.lower():
        return "오늘 식단을 분석한 결과, 전반적으로 균형 잡힌 하루를 보내셨네요! 단백질 섭취를 조금 더 늘리면 목표 달성에 가까워질 거예요."
    # ──────────────────────────────────────────────────────

    if "조정" in prompt or "adjust" in prompt.lower():
        ...  # 기존 내용 유지
```

수정 후 전체 `_mock_response` 함수:

```python
def _mock_response(prompt: str) -> str:
    """개발용 mock 응답 (크레딧 절약). JSON 요청 여부에 따라 형식 분리."""
    if "영양 균형" in prompt or "diet" in prompt.lower():
        return "오늘 식단을 분석한 결과, 전반적으로 균형 잡힌 하루를 보내셨네요! 단백질 섭취를 조금 더 늘리면 목표 달성에 가까워질 거예요."

    if "조정" in prompt or "adjust" in prompt.lower():
        return "분석 결과를 반영했습니다. 꾸준한 노력이 빛을 발하고 있어요! 다음 주도 현재 강도를 유지하며 도전해보세요."

    if "루틴" in prompt or "routine" in prompt.lower():
        return json.dumps({
            "routine_name": "4일 상체/하체 분할 루틴",
            "days": [
                {
                    "day_label": "상체",
                    "exercises": [
                        {"name": "벤치프레스", "sets": 4, "reps": 8, "weight_kg": 60.0},
                        {"name": "덤벨 숄더프레스", "sets": 3, "reps": 10, "weight_kg": 18.0},
                        {"name": "랫풀다운", "sets": 3, "reps": 10, "weight_kg": 45.0}
                    ]
                },
                {
                    "day_label": "하체",
                    "exercises": [
                        {"name": "바벨 스쿼트", "sets": 4, "reps": 8, "weight_kg": 80.0},
                        {"name": "레그프레스", "sets": 3, "reps": 12, "weight_kg": 120.0},
                        {"name": "루마니안 데드리프트", "sets": 3, "reps": 10, "weight_kg": 60.0}
                    ]
                },
                {
                    "day_label": "상체",
                    "exercises": [
                        {"name": "인클라인 벤치프레스", "sets": 3, "reps": 10, "weight_kg": 50.0},
                        {"name": "바벨 로우", "sets": 4, "reps": 8, "weight_kg": 55.0},
                        {"name": "바벨 컬", "sets": 3, "reps": 12, "weight_kg": 25.0}
                    ]
                },
                {
                    "day_label": "하체",
                    "exercises": [
                        {"name": "핵 스쿼트", "sets": 4, "reps": 10, "weight_kg": 70.0},
                        {"name": "레그 컬", "sets": 3, "reps": 12, "weight_kg": 40.0},
                        {"name": "카프레이즈", "sets": 4, "reps": 15, "weight_kg": 0.0}
                    ]
                }
            ],
            "ai_comment": "근육량 증가를 위해 복합운동 위주로 구성했습니다. 점진적으로 무게를 늘려보세요!"
        }, ensure_ascii=False)

    if "JSON" in prompt or "json" in prompt:
        return (
            '[{"name":"닭가슴살 샐러드","kcal":380,"protein_g":42,"carb_g":18,"fat_g":12,'
            '"reason":"단백질 보충에 최적"},'
            '{"name":"두부된장찌개+현미밥","kcal":420,"protein_g":22,"carb_g":58,"fat_g":9,'
            '"reason":"균형잡힌 한식"},'
            '{"name":"연어구이+고구마","kcal":390,"protein_g":35,"carb_g":32,"fat_g":14,'
            '"reason":"오메가3 + 복합 탄수화물"}]'
        )
    return "오늘 하루도 균형 잡힌 식단으로 건강 목표에 가까워지고 있어요! 단백질 섭취에 특히 신경 써보세요."
```

- [ ] **Step 2: mock 동작 확인**

```bash
cd ai && python -c "
import os; os.environ['ENV'] = 'dev'
from app.services.claude_service import _mock_response
r = _mock_response('오늘 영양 균형 분석 결과입니다')
print(r)
"
```

Expected: `오늘 식단을 분석한 결과...` 메시지 출력

- [ ] **Step 3: 커밋**

```bash
cd ai
git add app/services/claude_service.py
git commit -m "feat(meal): F701 claude_service mock에 식단 분석 분기 추가"
```

---

## Task 4: 엔드포인트 추가 + E2E 테스트

**Files:**
- Modify: `ai/app/routers/ai_meal.py`
- Create: `ai/tests/test_ai_meal_analyze.py`

- [ ] **Step 1: E2E 테스트 작성**

```python
# ai/tests/test_ai_meal_analyze.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "total_kcal": 1500.0,
    "total_protein_g": 100.0,
    "total_carb_g": 180.0,
    "total_fat_g": 50.0,
    "target_kcal": 2000.0,
    "target_protein_g": 150.0,
    "target_carb_g": 250.0,
    "target_fat_g": 60.0,
    "health_goal": "MUSCLE",
    "meal_date": "2026-06-15",
}


def test_분석_성공시_200과_유효한_구조_반환():
    response = client.post("/ai/meal/diet-analyze", json=VALID_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "calorie_rate" in data
    assert "protein_rate" in data
    assert "carb_rate" in data
    assert "fat_rate" in data
    assert "balance_score" in data
    assert "weak_nutrients" in data
    assert "excess_nutrients" in data
    assert "ai_comment" in data
    assert isinstance(data["weak_nutrients"], list)
    assert isinstance(data["excess_nutrients"], list)
    assert isinstance(data["ai_comment"], str)
    assert len(data["ai_comment"]) > 0


def test_달성률이_응답에_정확히_반영된다():
    # 칼로리 75% 달성 (1500/2000)
    response = client.post("/ai/meal/diet-analyze", json=VALID_REQUEST)
    data = response.json()
    assert data["calorie_rate"] == 75.0


def test_부족_영양소가_weak_nutrients에_포함된다():
    # protein: 100/150 = 66.7% < 80% → weak
    response = client.post("/ai/meal/diet-analyze", json=VALID_REQUEST)
    data = response.json()
    assert "protein" in data["weak_nutrients"]


def test_목표치_초과시_excess_nutrients에_포함된다():
    req = {**VALID_REQUEST, "total_fat_g": 80.0, "target_fat_g": 60.0}
    # fat: 80/60 = 133% > 120% → excess
    response = client.post("/ai/meal/diet-analyze", json=req)
    data = response.json()
    assert "fat" in data["excess_nutrients"]


def test_모든_목표_달성시_weak_excess_모두_빈리스트():
    perfect = {
        **VALID_REQUEST,
        "total_kcal": 2000.0, "total_protein_g": 150.0,
        "total_carb_g": 250.0, "total_fat_g": 60.0,
    }
    response = client.post("/ai/meal/diet-analyze", json=perfect)
    data = response.json()
    assert data["weak_nutrients"] == []
    assert data["excess_nutrients"] == []
    assert data["balance_score"] == 100.0
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_ai_meal_analyze.py -v
```

Expected: `404 Not Found` (엔드포인트 없음)

- [ ] **Step 3: ai_meal.py에 엔드포인트 추가**

기존 `ai_meal.py` 상단 import에 추가하고 엔드포인트를 파일 하단에 추가한다.

```python
# ai/app/routers/ai_meal.py 전체
import json

from fastapi import APIRouter, HTTPException
from app.schemas.meal import (
    LastRecommendRequest, LastRecommendResponse, MealRecommendation,
    DietAnalyzeRequest, DietAnalyzeResponse,
)
from app.services.claude_service import call_claude
from app.services.diet_service import calculate_diet_analysis

router = APIRouter(prefix="/ai/meal", tags=["AI Meal"])


@router.post("/last-recommend", response_model=LastRecommendResponse)
async def last_recommend(req: LastRecommendRequest):
    """
    F802 - 마지막 끼니 추천
    트리거 조건:
      - 잔여 칼로리가 1끼 분량(목표 ÷ 끼니 수) ± 20% 범위일 때
      - 오후 5시 이후 (Spring에서 시간 체크 후 호출)
      - 오늘 최소 1끼 이상 기록됨
    """
    if req.meal_count < 1:
        raise HTTPException(status_code=400, detail="오늘 기록된 끼니가 없어 추천을 생성할 수 없습니다.")

    remain_kcal    = req.target_kcal      - req.total_kcal
    remain_protein = req.target_protein_g - req.total_protein_g
    remain_carb    = req.target_carb_g    - req.total_carb_g
    remain_fat     = req.target_fat_g     - req.total_fat_g

    rates = {
        "protein": req.total_protein_g / req.target_protein_g if req.target_protein_g > 0 else 1,
        "carb":    req.total_carb_g    / req.target_carb_g    if req.target_carb_g    > 0 else 1,
        "fat":     req.total_fat_g     / req.target_fat_g     if req.target_fat_g     > 0 else 1,
    }
    priority = min(rates, key=rates.get)

    prompt = f"""
당신은 영양사 AI입니다. 아래 잔여 영양소를 채울 수 있는 저녁 식단 3가지를 추천해주세요.

[잔여 영양소]
- 칼로리: {remain_kcal:.0f} kcal
- 단백질: {remain_protein:.1f} g
- 탄수화물: {remain_carb:.1f} g
- 지방: {remain_fat:.1f} g
- 가장 부족한 영양소: {priority}

[조건]
- 과도하게 많은 양이 아닌 현실적인 한 끼 분량으로 제안
- 잔여 영양소를 대부분 채울 수 있는 식단 위주로 추천
- 각 식단은 음식명, 칼로리(kcal), 단백질(g), 탄수화물(g), 지방(g), 추천 이유를 포함

JSON 배열 형식으로만 응답하세요:
[
  {{"name": "음식명", "kcal": 숫자, "protein_g": 숫자, "carb_g": 숫자, "fat_g": 숫자, "reason": "이유"}},
  ...
]
"""

    _FALLBACK = [
        MealRecommendation(name="닭가슴살 샐러드",    kcal=380, protein_g=42, carb_g=18, fat_g=12, reason="단백질 보충에 최적"),
        MealRecommendation(name="두부된장찌개+현미밥", kcal=420, protein_g=22, carb_g=58, fat_g=9,  reason="균형잡힌 한식"),
        MealRecommendation(name="연어구이+고구마",     kcal=390, protein_g=35, carb_g=32, fat_g=14, reason="오메가3 + 복합 탄수화물"),
    ]

    try:
        raw = await call_claude(prompt)
        cleaned = raw.strip()
        if "```" in cleaned:
            parts = cleaned.split("```")
            cleaned = parts[1] if len(parts) > 1 else cleaned
            if cleaned.startswith("json"):
                cleaned = cleaned[4:]
        meals = json.loads(cleaned)
        recommendations = [MealRecommendation(**m) for m in meals]
    except Exception as e:
        print(f"[ai_meal] fallback 사용 — 원인: {type(e).__name__}: {e}")
        recommendations = _FALLBACK

    return LastRecommendResponse(
        recommendations=recommendations,
        priority_nutrient=priority,
        ai_comment=f"오늘 {priority} 섭취가 가장 부족합니다. 아래 식단으로 하루를 마무리해보세요!",
    )


@router.post("/diet-analyze", response_model=DietAnalyzeResponse)
async def diet_analyze(req: DietAnalyzeRequest):
    """
    F701 - 식단 영양 균형 분석
    Spring이 오늘의 식단 합산값과 목표값을 전송하면
    달성률·균형 점수·부족/과다 영양소·AI 코멘트를 반환한다.
    """
    rates, balance_score, weak, excess = calculate_diet_analysis(
        total_kcal=req.total_kcal,
        total_protein_g=req.total_protein_g,
        total_carb_g=req.total_carb_g,
        total_fat_g=req.total_fat_g,
        target_kcal=req.target_kcal,
        target_protein_g=req.target_protein_g,
        target_carb_g=req.target_carb_g,
        target_fat_g=req.target_fat_g,
    )

    weak_label   = "·".join(weak)   if weak   else "없음"
    excess_label = "·".join(excess) if excess else "없음"
    prompt = (
        f"사용자 목표: {req.health_goal}, 날짜: {req.meal_date}\n"
        f"영양 균형 분석 — 균형 점수: {balance_score:.0f}/100\n"
        f"달성률: 칼로리 {rates['calorie_rate']:.0f}%, 단백질 {rates['protein_rate']:.0f}%, "
        f"탄수화물 {rates['carb_rate']:.0f}%, 지방 {rates['fat_rate']:.0f}%\n"
        f"부족: {weak_label} / 과다: {excess_label}\n"
        "위 결과를 바탕으로 사용자에게 2~3문장으로 격려와 개선 방향을 한국어로 제안하세요."
    )

    ai_comment = await call_claude(prompt, max_tokens=300)

    return DietAnalyzeResponse(
        calorie_rate=rates["calorie_rate"],
        protein_rate=rates["protein_rate"],
        carb_rate=rates["carb_rate"],
        fat_rate=rates["fat_rate"],
        balance_score=balance_score,
        weak_nutrients=weak,
        excess_nutrients=excess,
        ai_comment=ai_comment,
    )
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_ai_meal_analyze.py -v
```

Expected: `5 passed`

- [ ] **Step 5: 기존 테스트 회귀 확인**

```bash
cd ai && python -m pytest tests/ -v --ignore=tests/test_gms_api.py --ignore=tests/test_mfds_api.py
```

Expected: 전체 통과 (GMS·MFDS 실제 API 테스트는 제외)

- [ ] **Step 6: 커밋**

```bash
cd ai
git add app/routers/ai_meal.py tests/test_ai_meal_analyze.py
git commit -m "feat(meal): F701 POST /ai/meal/diet-analyze 구현"
```

---

## 완료 체크리스트

- [ ] `POST /ai/meal/diet-analyze` → 200, 달성률·균형 점수·부족/과다 목록 반환
- [ ] 목표치 0인 영양소 → 달성률 100% 처리 (ZeroDivision 없음)
- [ ] 달성률 80% 미만 → `weak_nutrients`에 포함
- [ ] 달성률 120% 초과 → `excess_nutrients`에 포함
- [ ] `ENV=dev` mock 모드에서 AI 코멘트 정상 반환
- [ ] 기존 `/ai/meal/last-recommend` 테스트 회귀 없음
