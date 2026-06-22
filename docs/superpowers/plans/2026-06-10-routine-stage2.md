# 루틴 2단계 구현 계획: F903 세션 기록 + F904 AI 루틴 자동 조정

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 운동 세션을 기록하고, 완료 직후 FastAPI `/ai/routine/adjust`를 @Async 비동기 호출하여 다음 주차 RoutineExercise(weekNumber+1)를 자동 생성한다.

**Architecture:** Spring이 RoutineSession + SessionSet 저장 후 @Async로 FastAPI 호출. FastAPI는 최근 4회 세션을 분석해 UP/HOLD/DOWN/VOLUME_UP/DELOAD 판단. 조정 결과를 Spring이 새 weekNumber의 RoutineExercise로 저장. `GET /routines/{id}/weekly-plan/{week}`로 특정 주차 운동 조회.

**Tech Stack:** Spring Boot 3.3 / Java 21 / JPA / RestClient / @Async / FastAPI / Pydantic / Claude API (mock 기본)

---

## 파일 맵

### FastAPI (신규·수정)
| 경로 | 작업 |
|---|---|
| `ai/app/schemas/routine.py` | 수정 — adjust 요청·응답 스키마 추가 |
| `ai/app/services/routine_service.py` | 신규 — 조정 계산 순수 함수 |
| `ai/app/services/claude_service.py` | 수정 — adjust mock 추가 |
| `ai/app/routers/ai_routine.py` | 수정 — POST /ai/routine/adjust 추가 |
| `ai/tests/test_routine_service.py` | 신규 |
| `ai/tests/test_ai_routine_adjust.py` | 신규 |

### Spring Boot (신규·수정)
| 경로 | 작업 |
|---|---|
| `routine/domain/RoutineExercise.java` | 수정 — weekNumber 필드 + 오버로드 create 추가 |
| `routine/application/RoutineResult.java` | 수정 — ExerciseResult에 weekNumber 추가 |
| `routine/presentation/RoutineResponse.java` | 수정 — ExerciseResponse에 weekNumber 추가 |
| `routine/domain/RoutineSession.java` | 신규 |
| `routine/domain/SessionSet.java` | 신규 |
| `routine/infrastructure/persistence/RoutineExerciseRepository.java` | 수정 — weekNumber 쿼리 추가 |
| `routine/infrastructure/persistence/RoutineSessionRepository.java` | 신규 |
| `routine/infrastructure/persistence/SessionSetRepository.java` | 신규 |
| `routine/infrastructure/client/AiRoutineAdjustClientRequest.java` | 신규 |
| `routine/infrastructure/client/AiRoutineAdjustClientResponse.java` | 신규 |
| `routine/infrastructure/client/AiRoutineClient.java` | 수정 — adjust 메서드 추가 |
| `global/config/AsyncConfig.java` | 신규 |
| `routine/application/SessionSetInput.java` | 신규 |
| `routine/application/RoutineSessionResult.java` | 신규 |
| `routine/application/RoutineSessionService.java` | 신규 |
| `routine/application/RoutineService.java` | 수정 — getWeeklyPlan 메서드 추가 |
| `routine/presentation/RoutineController.java` | 수정 — GET /weekly-plan/{week} 추가 |
| `routine/presentation/CreateSessionRequest.java` | 신규 |
| `routine/presentation/SessionResponse.java` | 신규 |
| `routine/presentation/RoutineSessionController.java` | 신규 |

### 테스트
| 경로 | 작업 |
|---|---|
| `routine/domain/RoutineSessionTest.java` | 신규 |
| `routine/application/RoutineSessionServiceTest.java` | 신규 |
| `routine/presentation/RoutineSessionControllerTest.java` | 신규 |

---

## Task 1: RoutineExercise weekNumber 추가

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/routine/domain/RoutineExercise.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineResult.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineResponse.java`

- [ ] **Step 1: RoutineExercise.java 수정**

기존 7-파라미터 `create()`는 그대로 두고(weekNumber=1 기본), 8-파라미터 오버로드 추가.

```java
// backend/src/main/java/com/ssafy/manager/routine/domain/RoutineExercise.java
package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routineId;
    private String dayLabel;
    private String exerciseName;
    private int targetSets;
    private int targetReps;
    private double targetWeightKg;
    private int orderIndex;
    private int weekNumber;

    private RoutineExercise(Long routineId, String dayLabel, String exerciseName,
                             int targetSets, int targetReps, double targetWeightKg,
                             int orderIndex, int weekNumber) {
        this.routineId = routineId;
        this.dayLabel = dayLabel;
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
        this.orderIndex = orderIndex;
        this.weekNumber = weekNumber;
    }

    public static RoutineExercise create(Long routineId, String dayLabel, String exerciseName,
                                          int targetSets, int targetReps, double targetWeightKg,
                                          int orderIndex) {
        return new RoutineExercise(routineId, dayLabel, exerciseName,
                targetSets, targetReps, targetWeightKg, orderIndex, 1);
    }

    public static RoutineExercise create(Long routineId, String dayLabel, String exerciseName,
                                          int targetSets, int targetReps, double targetWeightKg,
                                          int orderIndex, int weekNumber) {
        return new RoutineExercise(routineId, dayLabel, exerciseName,
                targetSets, targetReps, targetWeightKg, orderIndex, weekNumber);
    }

    public void update(String exerciseName, int targetSets, int targetReps, double targetWeightKg) {
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
    }
}
```

- [ ] **Step 2: RoutineResult.java 수정**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/RoutineResult.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.domain.RoutineExercise;

import java.util.List;

public record RoutineResult(
        Long routineId,
        String name,
        int daysPerWeek,
        boolean aiGenerated,
        List<ExerciseResult> exercises,
        String aiComment
) {
    public record ExerciseResult(
            Long id,
            String dayLabel,
            String exerciseName,
            int targetSets,
            int targetReps,
            double targetWeightKg,
            int orderIndex,
            int weekNumber
    ) {
        public static ExerciseResult from(RoutineExercise ex) {
            return new ExerciseResult(
                    ex.getId(), ex.getDayLabel(), ex.getExerciseName(),
                    ex.getTargetSets(), ex.getTargetReps(), ex.getTargetWeightKg(),
                    ex.getOrderIndex(), ex.getWeekNumber()
            );
        }
    }

    public static RoutineResult from(Routine routine, List<RoutineExercise> exercises, String aiComment) {
        return new RoutineResult(
                routine.getId(), routine.getName(), routine.getDaysPerWeek(),
                routine.isAiGenerated(),
                exercises.stream().map(ExerciseResult::from).toList(),
                aiComment
        );
    }
}
```

- [ ] **Step 3: RoutineResponse.java 수정**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineResponse.java
package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineResult;

import java.util.List;

public record RoutineResponse(
        Long routineId,
        String name,
        int daysPerWeek,
        boolean aiGenerated,
        List<ExerciseResponse> exercises,
        String aiComment
) {
    public record ExerciseResponse(
            Long id,
            String dayLabel,
            String exerciseName,
            int targetSets,
            int targetReps,
            double targetWeightKg,
            int orderIndex,
            int weekNumber
    ) {
        public static ExerciseResponse from(RoutineResult.ExerciseResult r) {
            return new ExerciseResponse(r.id(), r.dayLabel(), r.exerciseName(),
                    r.targetSets(), r.targetReps(), r.targetWeightKg(),
                    r.orderIndex(), r.weekNumber());
        }
    }

    public static RoutineResponse from(RoutineResult result) {
        return new RoutineResponse(
                result.routineId(), result.name(), result.daysPerWeek(), result.aiGenerated(),
                result.exercises().stream().map(ExerciseResponse::from).toList(),
                result.aiComment()
        );
    }
}
```

- [ ] **Step 4: 기존 테스트 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.*"
```

Expected: 기존 routine 테스트 전체 통과

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/domain/RoutineExercise.java \
        backend/src/main/java/com/ssafy/manager/routine/application/RoutineResult.java \
        backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineResponse.java
git commit -m "feat(routine): RoutineExercise weekNumber 추가 (AI 조정 버전 관리 기반)"
```

---

## Task 2: FastAPI — adjust 스키마

**Files:**
- Modify: `ai/app/schemas/routine.py`

- [ ] **Step 1: adjust 스키마 추가**

```python
# ai/app/schemas/routine.py
from pydantic import BaseModel
from typing import List


# ── generate 스키마 ──────────────────────────────────────────────────

class RoutineExerciseSchema(BaseModel):
    name: str
    sets: int
    reps: int
    weight_kg: float


class RoutineDaySchema(BaseModel):
    day_label: str
    exercises: List[RoutineExerciseSchema]


class RoutineGenerateRequest(BaseModel):
    gender: str
    age: int
    weight_kg: float
    height_cm: float
    health_goal: str
    days_per_week: int
    split_type: str
    split_labels: List[str]


class RoutineGenerateResponse(BaseModel):
    routine_name: str
    days: List[RoutineDaySchema]
    ai_comment: str


# ── adjust 스키마 ────────────────────────────────────────────────────

class ExerciseInfo(BaseModel):
    exercise_id: int
    day_label: str
    exercise_name: str
    target_sets: int
    target_reps: int
    target_weight_kg: float
    order_index: int


class SessionSetData(BaseModel):
    exercise_id: int
    exercise_name: str
    target_sets: int
    actual_sets_completed: int
    avg_actual_reps: float
    avg_actual_weight_kg: float


class RecentSessionData(BaseModel):
    session_date: str
    sets: List[SessionSetData]


class RoutineAdjustRequest(BaseModel):
    routine_id: int
    current_week_number: int
    exercises: List[ExerciseInfo]
    recent_sessions: List[RecentSessionData]


class ExerciseAdjustment(BaseModel):
    exercise_id: int
    action: str  # UP | HOLD | DOWN | VOLUME_UP | DELOAD
    new_weight_kg: float
    new_sets: int
    new_reps: int
    reason: str


class RoutineAdjustResponse(BaseModel):
    adjustments: List[ExerciseAdjustment]
    ai_comment: str
    next_week_number: int
```

- [ ] **Step 2: import 확인**

```bash
cd ai && python -c "from app.schemas.routine import RoutineAdjustRequest, RoutineAdjustResponse; print('OK')"
```

Expected: OK

- [ ] **Step 3: 커밋**

```bash
git add ai/app/schemas/routine.py
git commit -m "feat(routine): FastAPI adjust 요청·응답 스키마 추가"
```

---

## Task 3: FastAPI — routine_service.py (조정 계산)

**Files:**
- Create: `ai/app/services/routine_service.py`
- Create: `ai/tests/test_routine_service.py`

- [ ] **Step 1: 테스트 작성**

```python
# ai/tests/test_routine_service.py
from app.services.routine_service import calculate_adjustment
from app.schemas.routine import SessionSetData, RecentSessionData


def _session(exercise_id: int, target: int, completed: int) -> RecentSessionData:
    return RecentSessionData(
        session_date="2026-06-01",
        sets=[SessionSetData(
            exercise_id=exercise_id, exercise_name="벤치프레스",
            target_sets=target, actual_sets_completed=completed,
            avg_actual_reps=8.0, avg_actual_weight_kg=60.0,
        )]
    )


def test_세션없으면_HOLD():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [])
    assert action == "HOLD"
    assert weight == 60.0
    assert sets == 4


def test_마지막_세션_전성공이면_UP():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [_session(1, 4, 4)])
    assert action == "UP"
    assert weight > 60.0


def test_마지막_세션_일부성공이면_HOLD():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [_session(1, 4, 2)])
    assert action == "HOLD"
    assert weight == 60.0


def test_2회_연속_실패면_DOWN():
    sessions = [_session(1, 4, 1), _session(1, 4, 1)]
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, sessions)
    assert action == "DOWN"
    assert weight < 60.0


def test_3회_연속_성공이면_VOLUME_UP():
    sessions = [_session(1, 4, 4), _session(1, 4, 4), _session(1, 4, 4)]
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, sessions)
    assert action == "VOLUME_UP"
    assert sets > 4


def test_4주_지속_감퇴면_DELOAD():
    sessions = [_session(1, 4, 4), _session(1, 4, 3), _session(1, 4, 2), _session(1, 4, 1)]
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, sessions)
    assert action == "DELOAD"
    assert weight < 60.0 * 0.7


def test_다른_운동_세션은_무시된다():
    action, weight, sets, reps = calculate_adjustment(1, 4, 8, 60.0, [_session(99, 4, 4)])
    assert action == "HOLD"
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_routine_service.py -v
```

Expected: ImportError (모듈 없음)

- [ ] **Step 3: routine_service.py 구현**

```python
# ai/app/services/routine_service.py
from typing import List, Tuple
from app.schemas.routine import RecentSessionData


def calculate_adjustment(
    exercise_id: int,
    target_sets: int,
    target_reps: int,
    target_weight_kg: float,
    recent_sessions: List[RecentSessionData],
) -> Tuple[str, float, int, int]:
    """Returns (action, new_weight_kg, new_sets, new_reps)"""
    exercise_sessions = []
    for session in recent_sessions:
        for s in session.sets:
            if s.exercise_id == exercise_id:
                exercise_sessions.append(s)
                break

    if not exercise_sessions:
        return "HOLD", target_weight_kg, target_sets, target_reps

    rates = [s.actual_sets_completed / s.target_sets for s in exercise_sessions]

    if len(rates) >= 4 and rates[-1] < rates[0] * 0.6 and rates[-1] < 0.6:
        return "DELOAD", round(target_weight_kg * 0.6, 1), max(1, target_sets - 1), target_reps

    consecutive_success = 0
    consecutive_failure = 0
    for rate in reversed(rates):
        if rate >= 1.0:
            if consecutive_failure > 0:
                break
            consecutive_success += 1
        elif rate < 0.5:
            if consecutive_success > 0:
                break
            consecutive_failure += 1
        else:
            break

    if consecutive_success >= 3:
        return "VOLUME_UP", round(target_weight_kg + 5.0, 1), target_sets + 1, target_reps

    if consecutive_failure >= 2:
        return "DOWN", round(target_weight_kg * 0.9, 1), target_sets, target_reps

    if rates[-1] >= 1.0:
        increment = 2.5 if target_weight_kg < 30 else 5.0
        return "UP", round(target_weight_kg + increment, 1), target_sets, target_reps

    return "HOLD", target_weight_kg, target_sets, target_reps
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_routine_service.py -v
```

Expected: 7 tests passed

- [ ] **Step 5: 커밋**

```bash
git add ai/app/services/routine_service.py ai/tests/test_routine_service.py
git commit -m "feat(routine): FastAPI 루틴 조정 계산 서비스 (UP/HOLD/DOWN/VOLUME_UP/DELOAD)"
```

---

## Task 4: FastAPI — claude_service mock + adjust 엔드포인트

**Files:**
- Modify: `ai/app/services/claude_service.py`
- Modify: `ai/app/routers/ai_routine.py`
- Create: `ai/tests/test_ai_routine_adjust.py`

- [ ] **Step 1: 테스트 작성**

```python
# ai/tests/test_ai_routine_adjust.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "routine_id": 1,
    "current_week_number": 1,
    "exercises": [
        {"exercise_id": 1, "day_label": "상체", "exercise_name": "벤치프레스",
         "target_sets": 4, "target_reps": 8, "target_weight_kg": 60.0, "order_index": 0}
    ],
    "recent_sessions": [
        {"session_date": "2026-06-03", "sets": [
            {"exercise_id": 1, "exercise_name": "벤치프레스",
             "target_sets": 4, "actual_sets_completed": 4,
             "avg_actual_reps": 8.0, "avg_actual_weight_kg": 60.0}
        ]}
    ]
}


def test_adjust_성공시_200과_유효한_구조_반환():
    response = client.post("/ai/routine/adjust", json=VALID_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "adjustments" in data
    assert "ai_comment" in data
    assert data["next_week_number"] == 2
    assert len(data["adjustments"]) == 1
    adj = data["adjustments"][0]
    assert adj["exercise_id"] == 1
    assert adj["action"] in {"UP", "HOLD", "DOWN", "VOLUME_UP", "DELOAD"}
    assert "new_weight_kg" in adj
    assert "new_sets" in adj
    assert "new_reps" in adj
    assert "reason" in adj


def test_세션없으면_HOLD_반환():
    req = {**VALID_REQUEST, "recent_sessions": []}
    response = client.post("/ai/routine/adjust", json=req)
    assert response.status_code == 200
    assert response.json()["adjustments"][0]["action"] == "HOLD"
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_ai_routine_adjust.py -v
```

Expected: FAIL (404 — /adjust 없음)

- [ ] **Step 3: claude_service.py에 adjust mock 분기 추가**

```python
# ai/app/services/claude_service.py
import httpx
from app.config import settings


async def call_claude(prompt: str, model: str | None = None, max_tokens: int = 1000) -> str:
    if settings.env == "dev":
        return _mock_response(prompt)
    return await _call_gms(prompt, model=model or settings.default_model, max_tokens=max_tokens)


async def _call_gms(prompt: str, model: str, max_tokens: int) -> str:
    url = f"{settings.gms_base_url}/v1/messages"
    headers = {
        "Content-Type": "application/json",
        "x-api-key": settings.gms_api_key,
        "anthropic-version": settings.anthropic_version,
    }
    payload = {
        "model": model,
        "max_tokens": max_tokens,
        "messages": [{"role": "user", "content": prompt}],
    }
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post(url, headers=headers, json=payload)
        response.raise_for_status()
    data = response.json()
    return data["content"][0]["text"]


def _mock_response(prompt: str) -> str:
    if "조정" in prompt or "adjust" in prompt.lower():
        return "분석 결과를 반영했습니다. 꾸준한 노력이 빛을 발하고 있어요! 다음 주도 현재 강도를 유지하며 도전해보세요."

    if "루틴" in prompt or "routine" in prompt.lower():
        import json
        return json.dumps({
            "routine_name": "4일 상체/하체 분할 루틴",
            "days": [
                {"day_label": "상체", "exercises": [
                    {"name": "벤치프레스", "sets": 4, "reps": 8, "weight_kg": 60.0},
                    {"name": "덤벨 숄더프레스", "sets": 3, "reps": 10, "weight_kg": 18.0}
                ]},
                {"day_label": "하체", "exercises": [
                    {"name": "바벨 스쿼트", "sets": 4, "reps": 8, "weight_kg": 80.0},
                    {"name": "레그프레스", "sets": 3, "reps": 12, "weight_kg": 120.0}
                ]},
                {"day_label": "상체", "exercises": [
                    {"name": "인클라인 벤치프레스", "sets": 3, "reps": 10, "weight_kg": 50.0},
                    {"name": "바벨 로우", "sets": 4, "reps": 8, "weight_kg": 55.0}
                ]},
                {"day_label": "하체", "exercises": [
                    {"name": "핵 스쿼트", "sets": 4, "reps": 10, "weight_kg": 70.0},
                    {"name": "레그 컬", "sets": 3, "reps": 12, "weight_kg": 40.0}
                ]}
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

- [ ] **Step 4: ai_routine.py에 adjust 엔드포인트 추가**

```python
# ai/app/routers/ai_routine.py
import json
from fastapi import APIRouter, HTTPException
from app.schemas.routine import (
    RoutineGenerateRequest, RoutineGenerateResponse,
    RoutineAdjustRequest, RoutineAdjustResponse, ExerciseAdjustment,
)
from app.services.claude_service import call_claude
from app.services.routine_service import calculate_adjustment

router = APIRouter(prefix="/ai/routine", tags=["AI Routine"])

_REASON = {
    "UP":         "모든 세트·반복 성공 → 무게 증량",
    "HOLD":       "일부 세트 성공 → 무게 유지",
    "DOWN":       "2회 연속 실패 → 무게 감량",
    "VOLUME_UP":  "3회 연속 성공 → 볼륨 증가",
    "DELOAD":     "4주 누적 피로 → 디로드 적용",
}


@router.post("/generate", response_model=RoutineGenerateResponse)
async def generate_routine(req: RoutineGenerateRequest):
    prompt = f"""
사용자 정보: 성별={req.gender}, 나이={req.age}세, 키={req.height_cm}cm, 몸무게={req.weight_kg}kg, 목표={req.health_goal}
운동 분할: 주 {req.days_per_week}회 [{', '.join(req.split_labels)}]

각 분할에 맞는 운동 루틴을 반드시 아래 JSON 형식으로만 응답하세요. 추가 설명 없이 JSON만 반환하세요:
{{
  "routine_name": "루틴 이름",
  "days": [
    {{
      "day_label": "분할명",
      "exercises": [
        {{"name": "운동명", "sets": 4, "reps": 8, "weight_kg": 60.0}}
      ]
    }}
  ],
  "ai_comment": "2~3문장 동기부여 코멘트"
}}
"""
    raw = await call_claude(prompt)
    try:
        data = json.loads(raw)
        return RoutineGenerateResponse(**data)
    except (json.JSONDecodeError, ValueError, TypeError) as e:
        raise HTTPException(status_code=500, detail=f"AI 응답 파싱 실패: {e}")


@router.post("/adjust", response_model=RoutineAdjustResponse)
async def adjust_routine(req: RoutineAdjustRequest):
    adjustments = []
    for ex in req.exercises:
        action, new_weight, new_sets, new_reps = calculate_adjustment(
            ex.exercise_id, ex.target_sets, ex.target_reps,
            ex.target_weight_kg, req.recent_sessions,
        )
        adjustments.append(ExerciseAdjustment(
            exercise_id=ex.exercise_id,
            action=action,
            new_weight_kg=new_weight,
            new_sets=new_sets,
            new_reps=new_reps,
            reason=_REASON.get(action, action),
        ))

    ai_comment = await call_claude(
        f"루틴 조정 완료. {len(adjustments)}개 운동 분석. 격려 코멘트 한두 문장."
    )
    return RoutineAdjustResponse(
        adjustments=adjustments,
        ai_comment=ai_comment,
        next_week_number=req.current_week_number + 1,
    )
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_ai_routine_adjust.py tests/test_ai_routine.py -v
```

Expected: 4 tests passed

- [ ] **Step 6: 커밋**

```bash
git add ai/app/services/claude_service.py ai/app/routers/ai_routine.py \
        ai/tests/test_ai_routine_adjust.py
git commit -m "feat(routine): FastAPI POST /ai/routine/adjust 구현"
```

---

## Task 5: Spring — RoutineSession + SessionSet 도메인

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/domain/RoutineSession.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/domain/SessionSet.java`
- Create: `backend/src/test/java/com/ssafy/manager/routine/domain/RoutineSessionTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/routine/domain/RoutineSessionTest.java
package com.ssafy.manager.routine.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RoutineSessionTest {

    @Test
    void RoutineSession_create_팩토리로_생성된다() {
        RoutineSession session = RoutineSession.create(1L, 2L, LocalDate.of(2026, 6, 10));

        assertThat(session.getRoutineId()).isEqualTo(1L);
        assertThat(session.getMemberId()).isEqualTo(2L);
        assertThat(session.getSessionDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    void SessionSet_create_팩토리로_생성된다() {
        SessionSet set = SessionSet.create(1L, 10L, "벤치프레스", 1, 8, 60.0, true);

        assertThat(set.getSessionId()).isEqualTo(1L);
        assertThat(set.getExerciseId()).isEqualTo(10L);
        assertThat(set.getExerciseName()).isEqualTo("벤치프레스");
        assertThat(set.getSetNumber()).isEqualTo(1);
        assertThat(set.getActualReps()).isEqualTo(8);
        assertThat(set.getActualWeightKg()).isEqualTo(60.0);
        assertThat(set.isCompleted()).isTrue();
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.domain.RoutineSessionTest" 2>&1 | tail -10
```

Expected: 컴파일 오류 (클래스 없음)

- [ ] **Step 3: RoutineSession.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/domain/RoutineSession.java
package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routineId;
    private Long memberId;
    private LocalDate sessionDate;
    private LocalDateTime completedAt;

    private RoutineSession(Long routineId, Long memberId, LocalDate sessionDate) {
        this.routineId = routineId;
        this.memberId = memberId;
        this.sessionDate = sessionDate;
        this.completedAt = LocalDateTime.now();
    }

    public static RoutineSession create(Long routineId, Long memberId, LocalDate sessionDate) {
        return new RoutineSession(routineId, memberId, sessionDate);
    }
}
```

- [ ] **Step 4: SessionSet.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/domain/SessionSet.java
package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sessionId;
    private Long exerciseId;
    private String exerciseName;
    private int setNumber;
    private int actualReps;
    private double actualWeightKg;
    private boolean completed;

    private SessionSet(Long sessionId, Long exerciseId, String exerciseName,
                       int setNumber, int actualReps, double actualWeightKg, boolean completed) {
        this.sessionId = sessionId;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.setNumber = setNumber;
        this.actualReps = actualReps;
        this.actualWeightKg = actualWeightKg;
        this.completed = completed;
    }

    public static SessionSet create(Long sessionId, Long exerciseId, String exerciseName,
                                     int setNumber, int actualReps, double actualWeightKg,
                                     boolean completed) {
        return new SessionSet(sessionId, exerciseId, exerciseName,
                setNumber, actualReps, actualWeightKg, completed);
    }
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.domain.RoutineSessionTest"
```

Expected: 2 tests passed

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/domain/RoutineSession.java \
        backend/src/main/java/com/ssafy/manager/routine/domain/SessionSet.java \
        backend/src/test/java/com/ssafy/manager/routine/domain/RoutineSessionTest.java
git commit -m "feat(routine): RoutineSession, SessionSet 도메인 엔티티 추가"
```

---

## Task 6: Spring — 리포지토리

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineExerciseRepository.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/SessionSetRepository.java`

- [ ] **Step 1: RoutineExerciseRepository 업데이트**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineExerciseRepository.java
package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoutineExerciseRepository extends JpaRepository<RoutineExercise, Long> {

    List<RoutineExercise> findByRoutineIdOrderByDayLabelAscOrderIndexAsc(Long routineId);

    List<RoutineExercise> findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(
            Long routineId, int weekNumber);

    @Query("SELECT COALESCE(MAX(re.weekNumber), 1) FROM RoutineExercise re WHERE re.routineId = :routineId")
    int findMaxWeekNumberByRoutineId(@Param("routineId") Long routineId);
}
```

- [ ] **Step 2: RoutineSessionRepository.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineSessionRepository.java
package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {
    List<RoutineSession> findTop4ByRoutineIdOrderBySessionDateDesc(Long routineId);
}
```

- [ ] **Step 3: SessionSetRepository.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/SessionSetRepository.java
package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.SessionSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionSetRepository extends JpaRepository<SessionSet, Long> {
    List<SessionSet> findBySessionIdIn(List<Long> sessionIds);
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/
git commit -m "feat(routine): RoutineSession, SessionSet 리포지토리 + RoutineExercise weekNumber 쿼리"
```

---

## Task 7: Spring — AiRoutineAdjustClient

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineAdjustClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineAdjustClientResponse.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClient.java`

- [ ] **Step 1: AiRoutineAdjustClientRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineAdjustClientRequest.java
package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineAdjustClientRequest(
        @JsonProperty("routine_id") Long routineId,
        @JsonProperty("current_week_number") int currentWeekNumber,
        @JsonProperty("exercises") List<ExerciseInfo> exercises,
        @JsonProperty("recent_sessions") List<RecentSession> recentSessions
) {
    public record ExerciseInfo(
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("day_label") String dayLabel,
            @JsonProperty("exercise_name") String exerciseName,
            @JsonProperty("target_sets") int targetSets,
            @JsonProperty("target_reps") int targetReps,
            @JsonProperty("target_weight_kg") double targetWeightKg,
            @JsonProperty("order_index") int orderIndex
    ) {}

    public record RecentSession(
            @JsonProperty("session_date") String sessionDate,
            @JsonProperty("sets") List<SessionSetData> sets
    ) {}

    public record SessionSetData(
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("exercise_name") String exerciseName,
            @JsonProperty("target_sets") int targetSets,
            @JsonProperty("actual_sets_completed") int actualSetsCompleted,
            @JsonProperty("avg_actual_reps") double avgActualReps,
            @JsonProperty("avg_actual_weight_kg") double avgActualWeightKg
    ) {}
}
```

- [ ] **Step 2: AiRoutineAdjustClientResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineAdjustClientResponse.java
package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineAdjustClientResponse(
        @JsonProperty("adjustments") List<Adjustment> adjustments,
        @JsonProperty("ai_comment") String aiComment,
        @JsonProperty("next_week_number") int nextWeekNumber
) {
    public record Adjustment(
            @JsonProperty("exercise_id") Long exerciseId,
            @JsonProperty("action") String action,
            @JsonProperty("new_weight_kg") double newWeightKg,
            @JsonProperty("new_sets") int newSets,
            @JsonProperty("new_reps") int newReps,
            @JsonProperty("reason") String reason
    ) {}
}
```

- [ ] **Step 3: AiRoutineClient에 adjust 추가**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClient.java
package com.ssafy.manager.routine.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiRoutineClient {

    private final RestClient aiRoutineRestClient;

    public AiRoutineClientResponse generate(AiRoutineClientRequest request) {
        return aiRoutineRestClient.post()
                .uri("/ai/routine/generate")
                .body(request)
                .retrieve()
                .body(AiRoutineClientResponse.class);
    }

    public AiRoutineAdjustClientResponse adjust(AiRoutineAdjustClientRequest request) {
        return aiRoutineRestClient.post()
                .uri("/ai/routine/adjust")
                .body(request)
                .retrieve()
                .body(AiRoutineAdjustClientResponse.class);
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/
git commit -m "feat(routine): AiRoutineAdjustClientRequest/Response + AiRoutineClient.adjust 추가"
```

---

## Task 8: Spring — AsyncConfig + RoutineSessionService

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/global/config/AsyncConfig.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/application/SessionSetInput.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionResult.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java`
- Create: `backend/src/test/java/com/ssafy/manager/routine/application/RoutineSessionServiceTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/routine/application/RoutineSessionServiceTest.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientResponse;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoutineSessionServiceTest {

    @Mock RoutineRepository routineRepository;
    @Mock RoutineSessionRepository routineSessionRepository;
    @Mock SessionSetRepository sessionSetRepository;
    @Mock RoutineExerciseRepository routineExerciseRepository;
    @Mock AiRoutineClient aiRoutineClient;

    @InjectMocks RoutineSessionService routineSessionService;

    @Test
    void 세션_기록시_RoutineSession과_SessionSet이_저장된다() {
        given(routineRepository.existsById(1L)).willReturn(true);
        List<SessionSetInput> inputs = List.of(
                new SessionSetInput(10L, "벤치프레스", 1, 8, 60.0, true)
        );

        routineSessionService.recordSession(2L, 1L, LocalDate.of(2026, 6, 10), inputs);

        ArgumentCaptor<RoutineSession> captor = ArgumentCaptor.forClass(RoutineSession.class);
        verify(routineSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutineId()).isEqualTo(1L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(2L);
        verify(sessionSetRepository).saveAll(any());
    }

    @Test
    void 없는_루틴으로_세션_기록시_예외가_발생한다() {
        given(routineRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> routineSessionService.recordSession(
                2L, 1L, LocalDate.now(), List.of()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("루틴을 찾을 수 없습니다");
    }

    @Test
    void adjustAndSave_FastAPI응답으로_새_주차_운동이_저장된다() {
        RoutineExercise ex = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);
        given(routineExerciseRepository.findMaxWeekNumberByRoutineId(1L)).willReturn(1);
        given(routineExerciseRepository.findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(1L, 1))
                .willReturn(List.of(ex));
        given(routineSessionRepository.findTop4ByRoutineIdOrderBySessionDateDesc(1L)).willReturn(List.of());
        given(sessionSetRepository.findBySessionIdIn(any())).willReturn(List.of());
        given(aiRoutineClient.adjust(any())).willReturn(new AiRoutineAdjustClientResponse(
                List.of(new AiRoutineAdjustClientResponse.Adjustment(
                        ex.getId(), "UP", 62.5, 4, 8, "성공 → 증량")),
                "잘 하고 있어요!", 2
        ));

        routineSessionService.adjustAndSave(1L);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(routineExerciseRepository).saveAll(captor.capture());
        List<RoutineExercise> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getWeekNumber()).isEqualTo(2);
        assertThat(saved.get(0).getTargetWeightKg()).isEqualTo(62.5);
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.application.RoutineSessionServiceTest" 2>&1 | tail -15
```

Expected: 컴파일 오류

- [ ] **Step 3: AsyncConfig.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/global/config/AsyncConfig.java
package com.ssafy.manager.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
}
```

- [ ] **Step 4: SessionSetInput.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/SessionSetInput.java
package com.ssafy.manager.routine.application;

public record SessionSetInput(
        Long exerciseId,
        String exerciseName,
        int setNumber,
        int actualReps,
        double actualWeightKg,
        boolean completed
) {}
```

- [ ] **Step 5: RoutineSessionResult.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionResult.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RoutineSessionResult(
        Long sessionId,
        Long routineId,
        Long memberId,
        LocalDate sessionDate,
        LocalDateTime completedAt,
        List<SetResult> sets
) {
    public record SetResult(
            Long id,
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {
        public static SetResult from(SessionSet s) {
            return new SetResult(s.getId(), s.getExerciseId(), s.getExerciseName(),
                    s.getSetNumber(), s.getActualReps(), s.getActualWeightKg(), s.isCompleted());
        }
    }

    public static RoutineSessionResult from(RoutineSession session, List<SessionSet> sets) {
        return new RoutineSessionResult(
                session.getId(), session.getRoutineId(), session.getMemberId(),
                session.getSessionDate(), session.getCompletedAt(),
                sets.stream().map(SetResult::from).toList()
        );
    }
}
```

- [ ] **Step 6: RoutineSessionService.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.RoutineSession;
import com.ssafy.manager.routine.domain.SessionSet;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientRequest;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineAdjustClientResponse;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineSessionRepository;
import com.ssafy.manager.routine.infrastructure.persistence.SessionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineSessionService {

    private final RoutineRepository routineRepository;
    private final RoutineSessionRepository routineSessionRepository;
    private final SessionSetRepository sessionSetRepository;
    private final RoutineExerciseRepository routineExerciseRepository;
    private final AiRoutineClient aiRoutineClient;

    @Transactional
    public RoutineSessionResult recordSession(Long memberId, Long routineId,
                                               LocalDate sessionDate,
                                               List<SessionSetInput> setInputs) {
        if (!routineRepository.existsById(routineId)) {
            throw new NoSuchElementException("루틴을 찾을 수 없습니다.");
        }

        RoutineSession session = RoutineSession.create(routineId, memberId, sessionDate);
        routineSessionRepository.save(session);

        List<SessionSet> sets = setInputs.stream()
                .map(i -> SessionSet.create(session.getId(), i.exerciseId(), i.exerciseName(),
                        i.setNumber(), i.actualReps(), i.actualWeightKg(), i.completed()))
                .toList();
        sessionSetRepository.saveAll(sets);

        adjustAndSave(routineId);
        return RoutineSessionResult.from(session, sets);
    }

    @Async
    @Transactional
    public void adjustAndSave(Long routineId) {
        try {
            int currentWeek = routineExerciseRepository.findMaxWeekNumberByRoutineId(routineId);
            List<RoutineExercise> exercises =
                    routineExerciseRepository.findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(
                            routineId, currentWeek);
            if (exercises.isEmpty()) return;

            List<RoutineSession> recentSessions =
                    routineSessionRepository.findTop4ByRoutineIdOrderBySessionDateDesc(routineId);
            List<Long> sessionIds = recentSessions.stream().map(RoutineSession::getId).toList();
            List<SessionSet> allSets = sessionIds.isEmpty()
                    ? List.of()
                    : sessionSetRepository.findBySessionIdIn(sessionIds);

            Map<Long, List<SessionSet>> setsBySession =
                    allSets.stream().collect(Collectors.groupingBy(SessionSet::getSessionId));

            List<AiRoutineAdjustClientRequest.RecentSession> recentSessionData =
                    recentSessions.stream().map(rs -> {
                        List<SessionSet> sessionSets =
                                setsBySession.getOrDefault(rs.getId(), List.of());
                        Map<Long, List<SessionSet>> byExercise = sessionSets.stream()
                                .collect(Collectors.groupingBy(SessionSet::getExerciseId));
                        List<AiRoutineAdjustClientRequest.SessionSetData> setData =
                                byExercise.entrySet().stream().map(e -> {
                                    List<SessionSet> exSets = e.getValue();
                                    long completed = exSets.stream()
                                            .filter(SessionSet::isCompleted).count();
                                    double avgReps = exSets.stream()
                                            .mapToInt(SessionSet::getActualReps).average().orElse(0);
                                    double avgWeight = exSets.stream()
                                            .mapToDouble(SessionSet::getActualWeightKg).average().orElse(0);
                                    return new AiRoutineAdjustClientRequest.SessionSetData(
                                            e.getKey(), exSets.get(0).getExerciseName(),
                                            exSets.size(), (int) completed, avgReps, avgWeight
                                    );
                                }).toList();
                        return new AiRoutineAdjustClientRequest.RecentSession(
                                rs.getSessionDate().toString(), setData
                        );
                    }).toList();

            List<AiRoutineAdjustClientRequest.ExerciseInfo> exerciseInfos = exercises.stream()
                    .map(ex -> new AiRoutineAdjustClientRequest.ExerciseInfo(
                            ex.getId(), ex.getDayLabel(), ex.getExerciseName(),
                            ex.getTargetSets(), ex.getTargetReps(), ex.getTargetWeightKg(),
                            ex.getOrderIndex()
                    )).toList();

            AiRoutineAdjustClientResponse response = aiRoutineClient.adjust(
                    new AiRoutineAdjustClientRequest(routineId, currentWeek,
                            exerciseInfos, recentSessionData)
            );

            Map<Long, AiRoutineAdjustClientResponse.Adjustment> adjustMap =
                    response.adjustments().stream()
                            .collect(Collectors.toMap(
                                    AiRoutineAdjustClientResponse.Adjustment::exerciseId, a -> a));

            List<RoutineExercise> newExercises = new ArrayList<>();
            for (RoutineExercise ex : exercises) {
                AiRoutineAdjustClientResponse.Adjustment adj = adjustMap.get(ex.getId());
                double w = adj != null ? adj.newWeightKg() : ex.getTargetWeightKg();
                int s = adj != null ? adj.newSets() : ex.getTargetSets();
                int r = adj != null ? adj.newReps() : ex.getTargetReps();
                newExercises.add(RoutineExercise.create(
                        routineId, ex.getDayLabel(), ex.getExerciseName(),
                        s, r, w, ex.getOrderIndex(), response.nextWeekNumber()
                ));
            }
            routineExerciseRepository.saveAll(newExercises);

        } catch (Exception e) {
            log.warn("루틴 자동 조정 실패 routineId={}: {}", routineId, e.getMessage());
        }
    }
}
```

- [ ] **Step 7: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.application.RoutineSessionServiceTest"
```

Expected: 3 tests passed

- [ ] **Step 8: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/global/config/AsyncConfig.java \
        backend/src/main/java/com/ssafy/manager/routine/application/SessionSetInput.java \
        backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionResult.java \
        backend/src/main/java/com/ssafy/manager/routine/application/RoutineSessionService.java \
        backend/src/test/java/com/ssafy/manager/routine/application/RoutineSessionServiceTest.java
git commit -m "feat(routine): RoutineSessionService 구현 (@Async AI 루틴 자동 조정)"
```

---

## Task 9: Spring — RoutineService.getWeeklyPlan + RoutineController 업데이트

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineService.java`
- Modify: `backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineController.java`
- Modify: `backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineControllerTest.java`

- [ ] **Step 1: RoutineService에 getWeeklyPlan 추가**

`RoutineService.java` 하단에 아래 메서드를 추가한다:

```java
public List<RoutineResult.ExerciseResult> getWeeklyPlan(Long routineId, int week) {
    return routineExerciseRepository
            .findByRoutineIdAndWeekNumberOrderByDayLabelAscOrderIndexAsc(routineId, week)
            .stream()
            .map(RoutineResult.ExerciseResult::from)
            .toList();
}
```

- [ ] **Step 2: RoutineController에 GET /weekly-plan/{week} 추가**

`RoutineController.java` 하단에 아래 메서드를 추가한다:

```java
@GetMapping("/{routineId}/weekly-plan/{week}")
public List<RoutineResponse.ExerciseResponse> getWeeklyPlan(
        @PathVariable Long routineId,
        @PathVariable int week) {
    return routineService.getWeeklyPlan(routineId, week).stream()
            .map(RoutineResponse.ExerciseResponse::from)
            .toList();
}
```

- [ ] **Step 3: RoutineControllerTest에 weekly-plan 테스트 추가**

`RoutineControllerTest.java` 하단에 아래 테스트를 추가한다:

```java
@Test
void 주차별_플랜_조회_성공시_200_반환() throws Exception {
    List<RoutineResult.ExerciseResult> weeklyPlan = List.of(
            new RoutineResult.ExerciseResult(1L, "상체", "벤치프레스", 4, 8, 62.5, 0, 2)
    );
    given(routineService.getWeeklyPlan(1L, 2)).willReturn(weeklyPlan);

    mockMvc.perform(get("/routines/1/weekly-plan/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].exerciseName").value("벤치프레스"))
            .andExpect(jsonPath("$[0].weekNumber").value(2))
            .andExpect(jsonPath("$[0].targetWeightKg").value(62.5));
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.presentation.RoutineControllerTest"
```

Expected: 6 tests passed (기존 5 + 신규 1)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/application/RoutineService.java \
        backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineController.java \
        backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineControllerTest.java
git commit -m "feat(routine): GET /routines/{id}/weekly-plan/{week} 구현"
```

---

## Task 10: Spring — RoutineSessionController + DTOs

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/CreateSessionRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/SessionResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineSessionController.java`
- Create: `backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineSessionControllerTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineSessionControllerTest.java
package com.ssafy.manager.routine.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.routine.application.RoutineSessionResult;
import com.ssafy.manager.routine.application.RoutineSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutineSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoutineSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RoutineSessionService routineSessionService;

    private static final RoutineSessionResult RESULT = new RoutineSessionResult(
            1L, 1L, 2L,
            LocalDate.of(2026, 6, 10),
            LocalDateTime.of(2026, 6, 10, 20, 0),
            List.of(new RoutineSessionResult.SetResult(1L, 10L, "벤치프레스", 1, 8, 60.0, true))
    );

    @Test
    void 세션_기록_성공시_201_반환() throws Exception {
        given(routineSessionService.recordSession(any(), any(), any(), any())).willReturn(RESULT);

        String body = """
                {
                  "memberId": 2,
                  "routineId": 1,
                  "sessionDate": "2026-06-10",
                  "sets": [
                    {"exerciseId": 10, "exerciseName": "벤치프레스",
                     "setNumber": 1, "actualReps": 8, "actualWeightKg": 60.0, "completed": true}
                  ]
                }
                """;

        mockMvc.perform(post("/routines/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.sets").isArray())
                .andExpect(jsonPath("$.sets[0].exerciseName").value("벤치프레스"));
    }

    @Test
    void 없는_루틴으로_세션_기록시_404_반환() throws Exception {
        given(routineSessionService.recordSession(any(), any(), any(), any()))
                .willThrow(new NoSuchElementException("루틴을 찾을 수 없습니다."));

        String body = """
                {
                  "memberId": 2, "routineId": 99,
                  "sessionDate": "2026-06-10", "sets": []
                }
                """;

        mockMvc.perform(post("/routines/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.presentation.RoutineSessionControllerTest" 2>&1 | tail -10
```

Expected: 컴파일 오류 (RoutineSessionController 없음)

- [ ] **Step 3: CreateSessionRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/CreateSessionRequest.java
package com.ssafy.manager.routine.presentation;

import java.time.LocalDate;
import java.util.List;

public record CreateSessionRequest(
        Long memberId,
        Long routineId,
        LocalDate sessionDate,
        List<SetItem> sets
) {
    public record SetItem(
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {}
}
```

- [ ] **Step 4: SessionResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/SessionResponse.java
package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineSessionResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SessionResponse(
        Long sessionId,
        Long routineId,
        Long memberId,
        LocalDate sessionDate,
        LocalDateTime completedAt,
        List<SetResponse> sets
) {
    public record SetResponse(
            Long id,
            Long exerciseId,
            String exerciseName,
            int setNumber,
            int actualReps,
            double actualWeightKg,
            boolean completed
    ) {}

    public static SessionResponse from(RoutineSessionResult result) {
        return new SessionResponse(
                result.sessionId(), result.routineId(), result.memberId(),
                result.sessionDate(), result.completedAt(),
                result.sets().stream()
                        .map(s -> new SetResponse(s.id(), s.exerciseId(), s.exerciseName(),
                                s.setNumber(), s.actualReps(), s.actualWeightKg(), s.completed()))
                        .toList()
        );
    }
}
```

- [ ] **Step 5: RoutineSessionController.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineSessionController.java
package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.RoutineSessionService;
import com.ssafy.manager.routine.application.SessionSetInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineSessionController {

    private final RoutineSessionService routineSessionService;

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse recordSession(@RequestBody CreateSessionRequest request) {
        List<SessionSetInput> inputs = request.sets().stream()
                .map(s -> new SessionSetInput(s.exerciseId(), s.exerciseName(),
                        s.setNumber(), s.actualReps(), s.actualWeightKg(), s.completed()))
                .toList();
        return SessionResponse.from(routineSessionService.recordSession(
                request.memberId(), request.routineId(), request.sessionDate(), inputs));
    }
}
```

- [ ] **Step 6: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.presentation.RoutineSessionControllerTest"
```

Expected: 2 tests passed

- [ ] **Step 7: 전체 테스트 실행**

```bash
cd backend && ./gradlew test
```

Expected: 전체 기존 테스트 포함 통과

- [ ] **Step 8: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/presentation/CreateSessionRequest.java \
        backend/src/main/java/com/ssafy/manager/routine/presentation/SessionResponse.java \
        backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineSessionController.java \
        backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineSessionControllerTest.java
git commit -m "feat(routine): RoutineSessionController 구현 (POST /routines/sessions)"
```

---

## 완료 체크리스트

- [ ] FastAPI `POST /ai/routine/adjust` — mock 모드 정상 응답, UP/HOLD/DOWN/VOLUME_UP/DELOAD 반환
- [ ] Spring `POST /routines/sessions` — 세션·세트 저장 후 201 반환
- [ ] Spring `@Async adjustAndSave` — FastAPI 호출 후 weekNumber+1 RoutineExercise 저장
- [ ] Spring `GET /routines/{id}/weekly-plan/{week}` — 특정 주차 운동 목록 반환
- [ ] 없는 루틴 → 404
- [ ] FastAPI 장애 → 세션 저장은 성공, 조정 실패는 warn 로그만
- [ ] 전체 기존 테스트 통과
