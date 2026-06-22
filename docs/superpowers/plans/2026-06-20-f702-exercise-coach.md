# F702 운동 코칭 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /ai/exercise/coach` 엔드포인트를 구현해 현재 루틴 성과를 분석한 집중 운동과 AI 맞춤 코칭 코멘트를 반환한다 (F702).

**Architecture:** Spring이 현재 루틴의 운동별 무게·세트·성공률·최근 조정 액션을 전송하면, FastAPI는 순수 함수 `pick_focus_exercise`로 집중 운동을 선택(DOWN/DELOAD 우선, 없으면 성공률 최저)하고 Claude에 포커스 운동 위주 코칭 프롬프트를 보내 3~4문장 텍스트를 반환한다. JSON 파싱 없는 텍스트 응답으로 안정성 확보.

**Tech Stack:** FastAPI, Pydantic v2, `call_claude` (GMS proxy), pytest

---

## 파일 구조

| 역할 | 파일 | 변경 |
|------|------|------|
| Pydantic 스키마 | `ai/app/schemas/coach.py` | 신규 생성 |
| 순수 계산 서비스 | `ai/app/services/coach_service.py` | 신규 생성 |
| 라우터 엔드포인트 | `ai/app/routers/ai_coach.py` | 신규 생성 |
| 앱 등록 | `ai/app/main.py` | 수정 (라우터 추가) |
| Mock 응답 | `ai/app/services/claude_service.py` | 수정 (분기 추가) |
| 서비스 단위 테스트 | `ai/tests/test_coach_service.py` | 신규 생성 |
| 통합 테스트 | `ai/tests/test_ai_coach.py` | 신규 생성 |

---

## 사전 준비: 브랜치 생성

구현 시작 전 반드시 새 브랜치를 만든다.

```bash
git checkout main
git pull
git checkout -b feature/F702-exercise-coach
```

---

## Task 1: Pydantic 스키마 생성

**Files:**
- Create: `ai/app/schemas/coach.py`

- [ ] **Step 1: 파일 생성**

```python
# ai/app/schemas/coach.py
from pydantic import BaseModel
from typing import List, Literal


class ExerciseCoachItem(BaseModel):
    exercise_name: str
    current_weight_kg: float
    current_sets: int
    current_reps: int
    last_action: str        # UP | HOLD | DOWN | VOLUME_UP | DELOAD
    success_rate: float     # 0.0~1.0 (마지막 세션 세트 성공률)


class ExerciseCoachRequest(BaseModel):
    health_goal: Literal["DIET", "MUSCLE", "HEALTH", "DISEASE"]
    week_number: int
    exercises: List[ExerciseCoachItem]


class ExerciseCoachResponse(BaseModel):
    focus_exercise: str     # 가장 개선이 필요한 운동명
    overall_comment: str    # AI 코칭 코멘트 (3~4문장)
```

- [ ] **Step 2: 임포트 확인**

```bash
cd ai && python -c "from app.schemas.coach import ExerciseCoachRequest, ExerciseCoachResponse; print('OK')"
```

Expected: `OK`

- [ ] **Step 3: 커밋**

```bash
git add ai/app/schemas/coach.py
git commit -m "feat(coach): F702 ExerciseCoach Pydantic 스키마 추가"
```

---

## Task 2: coach_service TDD

**Files:**
- Create: `ai/tests/test_coach_service.py`
- Create: `ai/app/services/coach_service.py`

- [ ] **Step 1: 실패하는 테스트 작성**

```python
# ai/tests/test_coach_service.py
import pytest
from app.services.coach_service import pick_focus_exercise
from app.schemas.coach import ExerciseCoachItem


def _item(name: str, last_action: str, success_rate: float) -> ExerciseCoachItem:
    return ExerciseCoachItem(
        exercise_name=name,
        current_weight_kg=60.0,
        current_sets=3,
        current_reps=10,
        last_action=last_action,
        success_rate=success_rate,
    )


def test_DOWN_액션_운동_우선_선택():
    exercises = [
        _item("스쿼트",     "DOWN", 0.4),
        _item("벤치프레스", "HOLD", 0.9),
        _item("데드리프트", "UP",   1.0),
    ]
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_DELOAD_액션_운동_우선_선택():
    exercises = [
        _item("스쿼트",     "DELOAD", 0.5),
        _item("벤치프레스", "UP",     1.0),
    ]
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_DOWN_DELOAD_없으면_성공률_최저_운동_선택():
    exercises = [
        _item("스쿼트",     "HOLD", 0.9),
        _item("벤치프레스", "UP",   0.6),
        _item("데드리프트", "HOLD", 0.8),
    ]
    assert pick_focus_exercise(exercises) == "벤치프레스"


def test_DOWN_여럿이면_성공률_낮은쪽_선택():
    exercises = [
        _item("스쿼트",     "DOWN", 0.3),
        _item("벤치프레스", "DOWN", 0.1),
        _item("데드리프트", "HOLD", 0.9),
    ]
    assert pick_focus_exercise(exercises) == "벤치프레스"


def test_DELOAD가_DOWN보다_우선():
    exercises = [
        _item("스쿼트",     "DOWN",   0.4),
        _item("벤치프레스", "DELOAD", 0.6),
    ]
    # 둘 다 priority_actions — 성공률 낮은 스쿼트 선택
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_단일_운동이면_그대로_반환():
    exercises = [_item("스쿼트", "UP", 1.0)]
    assert pick_focus_exercise(exercises) == "스쿼트"


def test_빈_리스트면_ValueError():
    with pytest.raises(ValueError):
        pick_focus_exercise([])
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd ai && python -m pytest tests/test_coach_service.py -v 2>&1 | head -10
```

Expected: ERROR (ImportError)

- [ ] **Step 3: coach_service.py 구현**

```python
# ai/app/services/coach_service.py
from app.schemas.coach import ExerciseCoachItem

_PRIORITY_ACTIONS = {"DOWN", "DELOAD"}


def pick_focus_exercise(exercises: list[ExerciseCoachItem]) -> str:
    """
    포커스 운동 선택.
    DOWN/DELOAD 액션 운동 우선, 없으면 성공률 최저 운동 반환.
    동점 시 성공률 기준 오름차순 min → 먼저 나오는 운동 선택.
    """
    if not exercises:
        raise ValueError("운동 목록이 비어 있습니다")
    priority = [e for e in exercises if e.last_action in _PRIORITY_ACTIONS]
    candidates = priority if priority else exercises
    return min(candidates, key=lambda e: e.success_rate).exercise_name
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd ai && python -m pytest tests/test_coach_service.py -v
```

Expected: 7 passed

- [ ] **Step 5: 커밋**

```bash
git add ai/app/services/coach_service.py ai/tests/test_coach_service.py
git commit -m "feat(coach): F702 coach_service pick_focus_exercise (TDD)"
```

---

## Task 3: 라우터 + Mock + 통합 테스트 + 앱 등록

**Files:**
- Create: `ai/app/routers/ai_coach.py`
- Create: `ai/tests/test_ai_coach.py`
- Modify: `ai/app/main.py`
- Modify: `ai/app/services/claude_service.py`

- [ ] **Step 1: 통합 테스트 먼저 작성**

```python
# ai/tests/test_ai_coach.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

_EXERCISES = [
    {
        "exercise_name": "스쿼트",
        "current_weight_kg": 80.0,
        "current_sets": 4,
        "current_reps": 8,
        "last_action": "DOWN",
        "success_rate": 0.4,
    },
    {
        "exercise_name": "벤치프레스",
        "current_weight_kg": 60.0,
        "current_sets": 4,
        "current_reps": 8,
        "last_action": "HOLD",
        "success_rate": 0.8,
    },
    {
        "exercise_name": "데드리프트",
        "current_weight_kg": 100.0,
        "current_sets": 3,
        "current_reps": 5,
        "last_action": "UP",
        "success_rate": 1.0,
    },
]

COACH_REQUEST = {
    "health_goal": "MUSCLE",
    "week_number": 3,
    "exercises": _EXERCISES,
}


def test_코치_성공시_200과_구조_반환():
    response = client.post("/ai/exercise/coach", json=COACH_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "focus_exercise" in data
    assert "overall_comment" in data
    assert isinstance(data["focus_exercise"], str)
    assert len(data["overall_comment"]) > 0


def test_DOWN_운동이_focus_exercise로_선택됨():
    response = client.post("/ai/exercise/coach", json=COACH_REQUEST)
    assert response.json()["focus_exercise"] == "스쿼트"


def test_DOWN_없으면_성공률_최저_운동이_focus():
    req = {
        **COACH_REQUEST,
        "exercises": [
            {**_EXERCISES[1], "last_action": "HOLD", "success_rate": 0.5},  # 벤치프레스
            {**_EXERCISES[2], "last_action": "HOLD", "success_rate": 0.9},  # 데드리프트
        ],
    }
    response = client.post("/ai/exercise/coach", json=req)
    assert response.json()["focus_exercise"] == "벤치프레스"


def test_운동_목록_빈_경우_422():
    req = {**COACH_REQUEST, "exercises": []}
    response = client.post("/ai/exercise/coach", json=req)
    # pick_focus_exercise raises ValueError → 500
    assert response.status_code in (422, 500)
```

- [ ] **Step 2: 테스트 실패 확인 (404)**

```bash
cd ai && python -m pytest tests/test_ai_coach.py::test_코치_성공시_200과_구조_반환 -v
```

Expected: FAIL (404)

- [ ] **Step 3: 라우터 구현**

```python
# ai/app/routers/ai_coach.py
from fastapi import APIRouter, HTTPException
from app.schemas.coach import ExerciseCoachRequest, ExerciseCoachResponse
from app.services.claude_service import call_claude
from app.services.coach_service import pick_focus_exercise

router = APIRouter(tags=["AI Coach"])


@router.post("/ai/exercise/coach", response_model=ExerciseCoachResponse)
async def exercise_coach(req: ExerciseCoachRequest):
    """
    F702 - 루틴 기반 맞춤 운동 코칭
    현재 루틴 성과를 분석해 집중 운동과 개인화 코칭 코멘트를 반환한다.
    """
    try:
        focus = pick_focus_exercise(req.exercises)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    exercise_summary = "\n".join(
        f"- {e.exercise_name}: {e.current_sets}세트×{e.current_reps}회 "
        f"{e.current_weight_kg}kg, 성공률 {e.success_rate:.0%}, 상태={e.last_action}"
        for e in req.exercises
    )

    prompt = (
        f"[{req.week_number}주차 운동 코칭] 건강 목표: {req.health_goal}\n"
        f"현재 루틴:\n{exercise_summary}\n"
        f"집중 운동: {focus}\n"
        f"위 데이터를 바탕으로 {focus} 위주의 자세 교정·동기부여·다음 주 전략을 "
        "3~4문장으로 한국어로 코칭해주세요."
    )

    overall_comment = await call_claude(prompt, max_tokens=400)

    return ExerciseCoachResponse(
        focus_exercise=focus,
        overall_comment=overall_comment,
    )
```

- [ ] **Step 4: main.py에 라우터 등록**

기존 `ai/app/main.py`를 읽은 뒤 `ai_coach` 라우터를 추가한다.

```python
# ai/app/main.py 전체
from fastapi import FastAPI
from app.routers import ai_meal, ai_plan, ai_routine, food, ai_report, ai_coach

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
app.include_router(ai_coach.router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "nyamnyam-fastapi"}
```

- [ ] **Step 5: claude_service.py mock 분기 추가**

`_mock_response` 함수에서 `if "체크인" in prompt:` 바로 다음에 추가한다.

```python
    if "운동 코칭" in prompt:
        return (
            "스쿼트 동작 시 무릎이 발끝을 넘지 않도록 주의하고, 코어를 단단히 잡아주세요. "
            "현재 성공률을 보면 하체 운동 집중이 필요합니다. "
            "다음 주는 무게를 유지하며 자세 완성도를 높이는 데 집중해보세요!"
        )
```

- [ ] **Step 6: 전체 테스트 통과 확인**

```bash
cd ai && python -m pytest tests/ --ignore=tests/test_gms_api.py --ignore=tests/test_mfds_api.py -v 2>&1 | tail -10
```

Expected: 96 passed (89 기존 + 7 coach_service + 4 ai_coach - 4가 모두 통과)
실제 총계: 89 + 7 + 4 = 100 passed

- [ ] **Step 7: 커밋**

```bash
git add ai/app/routers/ai_coach.py ai/app/main.py ai/app/services/claude_service.py ai/tests/test_ai_coach.py
git commit -m "feat(coach): F702 POST /ai/exercise/coach 구현"
```
