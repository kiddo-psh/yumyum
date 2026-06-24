# 루틴 1단계 구현 계획: F901 루틴 등록 + AI 루틴 생성

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 루틴 없는 신규 사용자에게 AI가 split 기반 루틴을 생성해주고, 기존 루틴 보유 사용자는 직접 등록하며, 생성 후 운동 종목·무게·세트를 자유 수정할 수 있게 한다.

**Architecture:** FastAPI `POST /ai/routine/generate`가 Claude를 통해 운동 종목·세트·반복·무게를 생성하고, Spring Boot가 RestClient로 호출한 뒤 `Routine` + `RoutineExercise` 엔티티로 저장한다. SplitType(주 운동 횟수별 분할 옵션)은 Spring 상수로 관리하며 FastAPI에 `split_labels`로 전달한다.

**Tech Stack:** Spring Boot 3.3 / Java 21 / JPA / RestClient / FastAPI / Pydantic / Claude API (mock 모드 기본)

---

## 파일 맵

### FastAPI (신규·수정)
| 경로 | 작업 |
|---|---|
| `ai/app/schemas/routine.py` | 신규 — Pydantic 요청·응답 모델 |
| `ai/app/services/claude_service.py` | 수정 — 루틴 mock 응답 추가 |
| `ai/app/routers/ai_routine.py` | 신규 — `/ai/routine/generate` 라우터 |
| `ai/app/main.py` | 수정 — 라우터 등록 |
| `ai/tests/test_ai_routine.py` | 신규 — 라우터 테스트 |

### Spring Boot (신규·수정)
| 경로 | 작업 |
|---|---|
| `routine/domain/Routine.java` | 신규 |
| `routine/domain/RoutineExercise.java` | 신규 |
| `routine/domain/SplitType.java` | 신규 — split 레이블 매핑 포함 |
| `routine/infrastructure/persistence/RoutineRepository.java` | 신규 |
| `routine/infrastructure/persistence/RoutineExerciseRepository.java` | 신규 |
| `global/config/RestClientConfig.java` | 수정 — aiRoutineRestClient 빈 추가 |
| `routine/infrastructure/client/AiRoutineClientRequest.java` | 신규 |
| `routine/infrastructure/client/AiRoutineClientResponse.java` | 신규 (nested records 포함) |
| `routine/infrastructure/client/AiRoutineClient.java` | 신규 |
| `routine/application/ExerciseInput.java` | 신규 — 수동 등록용 입력 record |
| `routine/application/RoutineResult.java` | 신규 — 서비스 반환 record |
| `routine/application/RoutineService.java` | 신규 |
| `routine/presentation/CreateAiRoutineRequest.java` | 신규 |
| `routine/presentation/CreateManualRoutineRequest.java` | 신규 |
| `routine/presentation/UpdateRoutineExerciseRequest.java` | 신규 |
| `routine/presentation/RoutineResponse.java` | 신규 |
| `routine/presentation/RoutineController.java` | 신규 |

### 테스트
| 경로 | 작업 |
|---|---|
| `routine/domain/RoutineTest.java` | 신규 |
| `routine/application/RoutineServiceTest.java` | 신규 |
| `routine/presentation/RoutineControllerTest.java` | 신규 |

---

## Task 1: FastAPI — schemas/routine.py

**Files:**
- Create: `ai/app/schemas/routine.py`

- [ ] **Step 1: 파일 생성**

```python
# ai/app/schemas/routine.py
from pydantic import BaseModel
from typing import List


class RoutineExerciseSchema(BaseModel):
    name: str
    sets: int
    reps: int
    weight_kg: float


class RoutineDaySchema(BaseModel):
    day_label: str
    exercises: List[RoutineExerciseSchema]


class RoutineGenerateRequest(BaseModel):
    gender: str           # "M" | "F"
    age: int
    weight_kg: float
    height_cm: float
    health_goal: str      # "MUSCLE_GAIN" | "WEIGHT_LOSS" | "MAINTAIN"
    days_per_week: int
    split_type: str       # e.g. "UPPER_LOWER_4"
    split_labels: List[str]  # e.g. ["상체", "하체", "상체", "하체"]


class RoutineGenerateResponse(BaseModel):
    routine_name: str
    days: List[RoutineDaySchema]
    ai_comment: str
```

- [ ] **Step 2: 커밋**

```bash
git add ai/app/schemas/routine.py
git commit -m "feat(routine): FastAPI 루틴 Pydantic 스키마 정의"
```

---

## Task 2: FastAPI — claude_service.py mock 업데이트

**Files:**
- Modify: `ai/app/services/claude_service.py`

- [ ] **Step 1: 테스트 작성 — mock이 루틴 JSON 반환하는지 확인**

```python
# ai/tests/test_claude_mock.py
import os
os.environ.setdefault("ENV", "dev")

from app.services.claude_service import _mock_response


def test_루틴_프롬프트는_JSON_루틴을_반환한다():
    import json
    result = _mock_response("주 4회 루틴을 JSON으로 생성해주세요")
    data = json.loads(result)
    assert "routine_name" in data
    assert "days" in data
    assert len(data["days"]) > 0
    assert "ai_comment" in data


def test_식단_프롬프트는_기존_포맷을_유지한다():
    import json
    result = _mock_response("JSON 식단 추천해줘")
    items = json.loads(result)
    assert isinstance(items, list)
    assert "name" in items[0]
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_claude_mock.py -v
```

Expected: `test_루틴_프롬프트는_JSON_루틴을_반환한다` FAIL

- [ ] **Step 3: claude_service.py 수정**

`_mock_response` 함수 상단에 루틴 분기를 추가한다. `import json`도 파일 상단에 추가한다.

```python
# ai/app/services/claude_service.py
import json          # 추가
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

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_claude_mock.py -v
```

Expected: 2 PASSED

- [ ] **Step 5: 커밋**

```bash
git add ai/app/services/claude_service.py ai/tests/test_claude_mock.py
git commit -m "feat(routine): claude_service mock에 루틴 JSON 응답 추가"
```

---

## Task 3: FastAPI — ai_routine.py 라우터 + main.py 등록

**Files:**
- Create: `ai/app/routers/ai_routine.py`
- Modify: `ai/app/main.py`
- Create: `ai/tests/test_ai_routine.py`

- [ ] **Step 1: 테스트 작성**

```python
# ai/tests/test_ai_routine.py
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "gender": "M",
    "age": 25,
    "weight_kg": 75.0,
    "height_cm": 178.0,
    "health_goal": "MUSCLE_GAIN",
    "days_per_week": 4,
    "split_type": "UPPER_LOWER_4",
    "split_labels": ["상체", "하체", "상체", "하체"]
}


def test_루틴_생성_성공시_200과_유효한_구조_반환():
    response = client.post("/ai/routine/generate", json=VALID_REQUEST)
    assert response.status_code == 200
    data = response.json()
    assert "routine_name" in data
    assert "days" in data
    assert len(data["days"]) == 4
    assert "ai_comment" in data
    for day in data["days"]:
        assert "day_label" in day
        assert len(day["exercises"]) > 0
        for ex in day["exercises"]:
            assert "name" in ex
            assert "sets" in ex
            assert "reps" in ex
            assert "weight_kg" in ex


def test_split_labels_개수가_days_per_week와_일치한다():
    req = {**VALID_REQUEST, "days_per_week": 3, "split_labels": ["상체", "하체", "전신"]}
    response = client.post("/ai/routine/generate", json=req)
    assert response.status_code == 200
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd ai && python -m pytest tests/test_ai_routine.py -v
```

Expected: FAIL (404 — 라우터 없음)

- [ ] **Step 3: ai_routine.py 생성**

```python
# ai/app/routers/ai_routine.py
import json
from fastapi import APIRouter, HTTPException
from app.schemas.routine import RoutineGenerateRequest, RoutineGenerateResponse
from app.services.claude_service import call_claude

router = APIRouter(prefix="/ai/routine", tags=["AI Routine"])


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
```

- [ ] **Step 4: main.py에 라우터 등록**

```python
# ai/app/main.py
from fastapi import FastAPI
from app.routers import ai_meal, ai_plan, ai_routine, food

app = FastAPI(
    title="냠냠코치 AI Server",
    description="Spring Boot ↔ FastAPI AI 연동 서버",
    version="1.0.0",
)

app.include_router(ai_meal.router)
app.include_router(ai_plan.router)
app.include_router(ai_routine.router)
app.include_router(food.router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "nyamnyam-fastapi"}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
cd ai && python -m pytest tests/test_ai_routine.py -v
```

Expected: 2 PASSED

- [ ] **Step 6: 커밋**

```bash
git add ai/app/routers/ai_routine.py ai/app/main.py ai/tests/test_ai_routine.py
git commit -m "feat(routine): FastAPI POST /ai/routine/generate 구현"
```

---

## Task 4: Spring — Routine + RoutineExercise 도메인 엔티티

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/domain/Routine.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/domain/RoutineExercise.java`
- Create: `backend/src/test/java/com/ssafy/manager/routine/domain/RoutineTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/routine/domain/RoutineTest.java
package com.ssafy.manager.routine.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RoutineTest {

    @Test
    void Routine_create_팩토리로_생성된다() {
        Routine routine = Routine.create(1L, "내 루틴", 4, false);

        assertThat(routine.getMemberId()).isEqualTo(1L);
        assertThat(routine.getName()).isEqualTo("내 루틴");
        assertThat(routine.getDaysPerWeek()).isEqualTo(4);
        assertThat(routine.isAiGenerated()).isFalse();
        assertThat(routine.getCreatedAt()).isNotNull();
    }

    @Test
    void RoutineExercise_create_팩토리로_생성된다() {
        RoutineExercise ex = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);

        assertThat(ex.getRoutineId()).isEqualTo(1L);
        assertThat(ex.getDayLabel()).isEqualTo("상체");
        assertThat(ex.getExerciseName()).isEqualTo("벤치프레스");
        assertThat(ex.getTargetSets()).isEqualTo(4);
        assertThat(ex.getTargetReps()).isEqualTo(8);
        assertThat(ex.getTargetWeightKg()).isEqualTo(60.0);
        assertThat(ex.getOrderIndex()).isEqualTo(0);
    }

    @Test
    void RoutineExercise_update로_필드가_변경된다() {
        RoutineExercise ex = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);

        ex.update("인클라인 벤치프레스", 3, 10, 55.0);

        assertThat(ex.getExerciseName()).isEqualTo("인클라인 벤치프레스");
        assertThat(ex.getTargetSets()).isEqualTo(3);
        assertThat(ex.getTargetReps()).isEqualTo(10);
        assertThat(ex.getTargetWeightKg()).isEqualTo(55.0);
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.domain.RoutineTest" 2>&1 | tail -20
```

Expected: 컴파일 오류 (클래스 없음)

- [ ] **Step 3: Routine.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/domain/Routine.java
package com.ssafy.manager.routine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private String name;
    private int daysPerWeek;
    private boolean aiGenerated;
    private LocalDateTime createdAt;

    private Routine(Long memberId, String name, int daysPerWeek, boolean aiGenerated) {
        this.memberId = memberId;
        this.name = name;
        this.daysPerWeek = daysPerWeek;
        this.aiGenerated = aiGenerated;
        this.createdAt = LocalDateTime.now();
    }

    public static Routine create(Long memberId, String name, int daysPerWeek, boolean aiGenerated) {
        return new Routine(memberId, name, daysPerWeek, aiGenerated);
    }
}
```

- [ ] **Step 4: RoutineExercise.java 생성**

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

    private RoutineExercise(Long routineId, String dayLabel, String exerciseName,
                             int targetSets, int targetReps, double targetWeightKg, int orderIndex) {
        this.routineId = routineId;
        this.dayLabel = dayLabel;
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
        this.orderIndex = orderIndex;
    }

    public static RoutineExercise create(Long routineId, String dayLabel, String exerciseName,
                                          int targetSets, int targetReps, double targetWeightKg,
                                          int orderIndex) {
        return new RoutineExercise(routineId, dayLabel, exerciseName,
                targetSets, targetReps, targetWeightKg, orderIndex);
    }

    public void update(String exerciseName, int targetSets, int targetReps, double targetWeightKg) {
        this.exerciseName = exerciseName;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeightKg = targetWeightKg;
    }
}
```

- [ ] **Step 5: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.domain.RoutineTest"
```

Expected: 3 tests passed

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/domain/ \
        backend/src/test/java/com/ssafy/manager/routine/domain/
git commit -m "feat(routine): Routine, RoutineExercise 도메인 엔티티 추가"
```

---

## Task 5: Spring — SplitType enum

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/domain/SplitType.java`

- [ ] **Step 1: 파일 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/domain/SplitType.java
package com.ssafy.manager.routine.domain;

import java.util.Arrays;
import java.util.List;

public enum SplitType {
    FULL_BODY_2(2, List.of("전신", "전신")),
    FULL_BODY_3(3, List.of("전신", "전신", "전신")),
    UPPER_LOWER_FULL(3, List.of("상체", "하체", "전신")),
    UPPER_LOWER_4(4, List.of("상체", "하체", "상체", "하체")),
    PUSH_PULL_LEGS_UPPER(4, List.of("가슴/삼두", "등/이두", "하체", "어깨/팔")),
    PUSH_PULL_LEGS_UPPER_LOWER(5, List.of("가슴/삼두", "등/이두", "하체", "상체", "하체")),
    PPLFU(5, List.of("가슴/삼두", "등/이두", "하체", "전신", "상체"));

    private final int daysPerWeek;
    private final List<String> splitLabels;

    SplitType(int daysPerWeek, List<String> splitLabels) {
        this.daysPerWeek = daysPerWeek;
        this.splitLabels = splitLabels;
    }

    public int getDaysPerWeek() { return daysPerWeek; }
    public List<String> getSplitLabels() { return splitLabels; }
    public String getLabel() { return String.join("/", splitLabels); }

    public static List<SplitType> findByDaysPerWeek(int days) {
        return Arrays.stream(values())
                .filter(s -> s.daysPerWeek == days)
                .toList();
    }
}
```

- [ ] **Step 2: 동작 확인용 인라인 테스트 — RoutineTest.java에 추가**

`RoutineTest.java` 파일 하단에 아래 테스트를 추가한다:

```java
@Test
void SplitType_findByDaysPerWeek_주4회_옵션을_반환한다() {
    List<SplitType> options = SplitType.findByDaysPerWeek(4);

    assertThat(options).hasSize(2);
    assertThat(options).contains(SplitType.UPPER_LOWER_4, SplitType.PUSH_PULL_LEGS_UPPER);
}

@Test
void SplitType_getSplitLabels_레이블_개수가_daysPerWeek와_일치한다() {
    assertThat(SplitType.UPPER_LOWER_4.getSplitLabels()).hasSize(4);
    assertThat(SplitType.FULL_BODY_3.getSplitLabels()).hasSize(3);
}
```

- [ ] **Step 3: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.domain.RoutineTest"
```

Expected: 5 tests passed

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/domain/SplitType.java \
        backend/src/test/java/com/ssafy/manager/routine/domain/RoutineTest.java
git commit -m "feat(routine): SplitType enum 추가 (주 운동 횟수별 분할 옵션)"
```

---

## Task 6: Spring — Repositories

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineRepository.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineExerciseRepository.java`

- [ ] **Step 1: RoutineRepository.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineRepository.java
package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findByMemberId(Long memberId);
}
```

- [ ] **Step 2: RoutineExerciseRepository.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/persistence/RoutineExerciseRepository.java
package com.ssafy.manager.routine.infrastructure.persistence;

import com.ssafy.manager.routine.domain.RoutineExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineExerciseRepository extends JpaRepository<RoutineExercise, Long> {
    List<RoutineExercise> findByRoutineIdOrderByDayLabelAscOrderIndexAsc(Long routineId);
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/infrastructure/
git commit -m "feat(routine): RoutineRepository, RoutineExerciseRepository 추가"
```

---

## Task 7: Spring — RestClientConfig 업데이트 + AiRoutineClient

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClientResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClient.java`

- [ ] **Step 1: RestClientConfig에 aiRoutineRestClient 빈 추가**

```java
// backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java
package com.ssafy.manager.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient aiPlanRestClient(
            @Value("${ai.fastapi.url:http://localhost:8000}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    RestClient aiRoutineRestClient(
            @Value("${ai.fastapi.url:http://localhost:8000}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
```

- [ ] **Step 2: AiRoutineClientRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClientRequest.java
package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineClientRequest(
        @JsonProperty("gender") String gender,
        @JsonProperty("age") int age,
        @JsonProperty("weight_kg") double weightKg,
        @JsonProperty("height_cm") double heightCm,
        @JsonProperty("health_goal") String healthGoal,
        @JsonProperty("days_per_week") int daysPerWeek,
        @JsonProperty("split_type") String splitType,
        @JsonProperty("split_labels") List<String> splitLabels
) {}
```

- [ ] **Step 3: AiRoutineClientResponse.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/AiRoutineClientResponse.java
package com.ssafy.manager.routine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiRoutineClientResponse(
        @JsonProperty("routine_name") String routineName,
        @JsonProperty("days") List<Day> days,
        @JsonProperty("ai_comment") String aiComment
) {
    public record Day(
            @JsonProperty("day_label") String dayLabel,
            @JsonProperty("exercises") List<Exercise> exercises
    ) {}

    public record Exercise(
            @JsonProperty("name") String name,
            @JsonProperty("sets") int sets,
            @JsonProperty("reps") int reps,
            @JsonProperty("weight_kg") double weightKg
    ) {}
}
```

- [ ] **Step 4: AiRoutineClient.java 생성**

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
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java \
        backend/src/main/java/com/ssafy/manager/routine/infrastructure/client/
git commit -m "feat(routine): AiRoutineClient + RestClient 빈 추가"
```

---

## Task 8: Spring — ExerciseInput + RoutineResult

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/application/ExerciseInput.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineResult.java`

- [ ] **Step 1: ExerciseInput.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/ExerciseInput.java
package com.ssafy.manager.routine.application;

public record ExerciseInput(
        String dayLabel,
        String exerciseName,
        int targetSets,
        int targetReps,
        double targetWeightKg,
        int orderIndex
) {}
```

- [ ] **Step 2: RoutineResult.java 생성**

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
            int orderIndex
    ) {
        public static ExerciseResult from(RoutineExercise ex) {
            return new ExerciseResult(
                    ex.getId(), ex.getDayLabel(), ex.getExerciseName(),
                    ex.getTargetSets(), ex.getTargetReps(), ex.getTargetWeightKg(),
                    ex.getOrderIndex()
            );
        }
    }

    public static RoutineResult from(Routine routine, List<RoutineExercise> exercises, String aiComment) {
        return new RoutineResult(
                routine.getId(),
                routine.getName(),
                routine.getDaysPerWeek(),
                routine.isAiGenerated(),
                exercises.stream().map(ExerciseResult::from).toList(),
                aiComment
        );
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/application/
git commit -m "feat(routine): ExerciseInput, RoutineResult record 추가"
```

---

## Task 9: Spring — RoutineService

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/application/RoutineService.java`
- Create: `backend/src/test/java/com/ssafy/manager/routine/application/RoutineServiceTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/routine/application/RoutineServiceTest.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.member.domain.ActivityLevel;
import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.SplitType;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClientResponse;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock ProgramRepository programRepository;
    @Mock RoutineRepository routineRepository;
    @Mock RoutineExerciseRepository routineExerciseRepository;
    @Mock AiRoutineClient aiRoutineClient;

    @InjectMocks RoutineService routineService;

    private static final Long MEMBER_ID = 1L;
    private final Member member = new Member(Sex.MALE, 1995, 178.0, 75.0, ActivityLevel.MODERATELY_ACTIVE);

    private static final AiRoutineClientResponse AI_RESPONSE = new AiRoutineClientResponse(
            "4일 상체/하체 분할 루틴",
            List.of(
                    new AiRoutineClientResponse.Day("상체", List.of(
                            new AiRoutineClientResponse.Exercise("벤치프레스", 4, 8, 60.0)
                    )),
                    new AiRoutineClientResponse.Day("하체", List.of(
                            new AiRoutineClientResponse.Exercise("바벨 스쿼트", 4, 8, 80.0)
                    ))
            ),
            "열심히 해봐요!"
    );

    @Test
    void AI_루틴_생성시_Routine과_RoutineExercise가_저장된다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(programRepository.findByMemberIdAndStatus(MEMBER_ID, ProgramStatus.ACTIVE))
                .willReturn(Optional.empty());
        given(aiRoutineClient.generate(any())).willReturn(AI_RESPONSE);

        RoutineResult result = routineService.createAi(MEMBER_ID, 4, SplitType.UPPER_LOWER_4);

        ArgumentCaptor<Routine> routineCaptor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(routineCaptor.capture());
        assertThat(routineCaptor.getValue().isAiGenerated()).isTrue();
        assertThat(routineCaptor.getValue().getDaysPerWeek()).isEqualTo(4);

        verify(routineExerciseRepository).saveAll(any());
        assertThat(result.aiComment()).isEqualTo("열심히 해봐요!");
        assertThat(result.exercises()).isNotEmpty();
    }

    @Test
    void 수동_루틴_생성시_FastAPI를_호출하지_않는다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        routineService.createManual(MEMBER_ID, "내 루틴", 3, List.of(
                new ExerciseInput("상체", "벤치프레스", 4, 8, 60.0, 0)
        ));

        verifyNoInteractions(aiRoutineClient);
        verify(routineRepository).save(any());
        verify(routineExerciseRepository).saveAll(any());
    }

    @Test
    void 없는_회원으로_AI_루틴_생성시_예외가_발생한다() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.createAi(MEMBER_ID, 4, SplitType.UPPER_LOWER_4))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다");
    }

    @Test
    void 운동_수정시_필드가_변경된다() {
        RoutineExercise exercise = RoutineExercise.create(1L, "상체", "벤치프레스", 4, 8, 60.0, 0);
        given(routineRepository.existsById(1L)).willReturn(true);
        given(routineExerciseRepository.findById(1L)).willReturn(Optional.of(exercise));

        RoutineResult.ExerciseResult result =
                routineService.updateExercise(1L, 1L, "인클라인 벤치프레스", 3, 10, 55.0);

        assertThat(result.exerciseName()).isEqualTo("인클라인 벤치프레스");
        assertThat(result.targetSets()).isEqualTo(3);
        assertThat(result.targetWeightKg()).isEqualTo(55.0);
    }

    @Test
    void 없는_루틴_수정시_예외가_발생한다() {
        given(routineRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> routineService.updateExercise(99L, 1L, "벤치프레스", 4, 8, 60.0))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("루틴을 찾을 수 없습니다");
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.application.RoutineServiceTest" 2>&1 | tail -20
```

Expected: 컴파일 오류 (RoutineService 없음)

- [ ] **Step 3: RoutineService.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/application/RoutineService.java
package com.ssafy.manager.routine.application;

import com.ssafy.manager.member.domain.Member;
import com.ssafy.manager.member.domain.Sex;
import com.ssafy.manager.member.infrastructure.persistence.MemberRepository;
import com.ssafy.manager.program.domain.ProgramStatus;
import com.ssafy.manager.program.domain.ProgramType;
import com.ssafy.manager.program.infrastructure.persistence.ProgramRepository;
import com.ssafy.manager.routine.domain.Routine;
import com.ssafy.manager.routine.domain.RoutineExercise;
import com.ssafy.manager.routine.domain.SplitType;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClient;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClientRequest;
import com.ssafy.manager.routine.infrastructure.client.AiRoutineClientResponse;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineExerciseRepository;
import com.ssafy.manager.routine.infrastructure.persistence.RoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final MemberRepository memberRepository;
    private final ProgramRepository programRepository;
    private final RoutineRepository routineRepository;
    private final RoutineExerciseRepository routineExerciseRepository;
    private final AiRoutineClient aiRoutineClient;

    @Transactional
    public RoutineResult createAi(Long memberId, int daysPerWeek, SplitType splitType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        String healthGoal = programRepository
                .findByMemberIdAndStatus(memberId, ProgramStatus.ACTIVE)
                .map(p -> toHealthGoal(p.getType()))
                .orElse("MAINTAIN");

        AiRoutineClientRequest request = new AiRoutineClientRequest(
                member.getSex() == Sex.MALE ? "M" : "F",
                member.age(LocalDate.now().getYear()),
                member.getWeightKg(),
                member.getHeightCm(),
                healthGoal,
                daysPerWeek,
                splitType.name(),
                splitType.getSplitLabels()
        );

        AiRoutineClientResponse response = aiRoutineClient.generate(request);

        Routine routine = Routine.create(memberId, response.routineName(), daysPerWeek, true);
        routineRepository.save(routine);

        List<RoutineExercise> exercises = new ArrayList<>();
        for (AiRoutineClientResponse.Day day : response.days()) {
            for (int i = 0; i < day.exercises().size(); i++) {
                AiRoutineClientResponse.Exercise ex = day.exercises().get(i);
                exercises.add(RoutineExercise.create(
                        routine.getId(), day.dayLabel(), ex.name(),
                        ex.sets(), ex.reps(), ex.weightKg(), i
                ));
            }
        }
        routineExerciseRepository.saveAll(exercises);

        return RoutineResult.from(routine, exercises, response.aiComment());
    }

    @Transactional
    public RoutineResult createManual(Long memberId, String name, int daysPerWeek,
                                       List<ExerciseInput> inputs) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        Routine routine = Routine.create(memberId, name, daysPerWeek, false);
        routineRepository.save(routine);

        List<RoutineExercise> exercises = inputs.stream()
                .map(e -> RoutineExercise.create(routine.getId(), e.dayLabel(), e.exerciseName(),
                        e.targetSets(), e.targetReps(), e.targetWeightKg(), e.orderIndex()))
                .toList();
        routineExerciseRepository.saveAll(exercises);

        return RoutineResult.from(routine, exercises, null);
    }

    @Transactional
    public RoutineResult.ExerciseResult updateExercise(Long routineId, Long exerciseId,
                                                        String exerciseName, int targetSets,
                                                        int targetReps, double targetWeightKg) {
        if (!routineRepository.existsById(routineId)) {
            throw new NoSuchElementException("루틴을 찾을 수 없습니다.");
        }
        RoutineExercise exercise = routineExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("운동을 찾을 수 없습니다."));
        exercise.update(exerciseName, targetSets, targetReps, targetWeightKg);
        return RoutineResult.ExerciseResult.from(exercise);
    }

    private String toHealthGoal(ProgramType type) {
        return switch (type) {
            case DIET -> "WEIGHT_LOSS";
            case MUSCLE -> "MUSCLE_GAIN";
            case HEALTH, DISEASE -> "MAINTAIN";
        };
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.application.RoutineServiceTest"
```

Expected: 5 tests passed

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/application/RoutineService.java \
        backend/src/test/java/com/ssafy/manager/routine/application/RoutineServiceTest.java
git commit -m "feat(routine): RoutineService 구현 (AI 생성, 수동 등록, 운동 수정)"
```

---

## Task 10: Spring — RoutineController + DTOs

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/CreateAiRoutineRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/CreateManualRoutineRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/UpdateRoutineExerciseRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineController.java`
- Create: `backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineControllerTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/ssafy/manager/routine/presentation/RoutineControllerTest.java
package com.ssafy.manager.routine.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.manager.routine.application.RoutineResult;
import com.ssafy.manager.routine.application.RoutineService;
import com.ssafy.manager.routine.domain.SplitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutineController.class)
class RoutineControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RoutineService routineService;

    private static final RoutineResult RESULT = new RoutineResult(
            1L, "4일 상체/하체 분할 루틴", 4, true,
            List.of(new RoutineResult.ExerciseResult(1L, "상체", "벤치프레스", 4, 8, 60.0, 0)),
            "열심히 해봐요!"
    );

    @Test
    void AI_루틴_생성_성공시_201_반환() throws Exception {
        given(routineService.createAi(anyLong(), anyInt(), any())).willReturn(RESULT);

        mockMvc.perform(post("/routines/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAiRoutineRequest(1L, 4, SplitType.UPPER_LOWER_4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routineId").value(1))
                .andExpect(jsonPath("$.aiGenerated").value(true))
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.aiComment").value("열심히 해봐요!"));
    }

    @Test
    void 수동_루틴_생성_성공시_201_반환() throws Exception {
        RoutineResult manualResult = new RoutineResult(
                2L, "내 루틴", 3, false,
                List.of(new RoutineResult.ExerciseResult(2L, "상체", "벤치프레스", 4, 8, 70.0, 0)),
                null
        );
        given(routineService.createManual(anyLong(), anyString(), anyInt(), any()))
                .willReturn(manualResult);

        String body = """
                {
                  "memberId": 1,
                  "name": "내 루틴",
                  "daysPerWeek": 3,
                  "exercises": [
                    {"dayLabel":"상체","exerciseName":"벤치프레스","targetSets":4,"targetReps":8,"targetWeightKg":70.0,"orderIndex":0}
                  ]
                }
                """;

        mockMvc.perform(post("/routines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routineId").value(2))
                .andExpect(jsonPath("$.aiGenerated").value(false));
    }

    @Test
    void split_options_조회_성공() throws Exception {
        mockMvc.perform(get("/routines/split-options").param("daysPerWeek", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].splitType").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    void 운동_수정_성공시_200_반환() throws Exception {
        RoutineResult.ExerciseResult updated = new RoutineResult.ExerciseResult(
                1L, "상체", "인클라인 벤치프레스", 3, 10, 55.0, 0
        );
        given(routineService.updateExercise(anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyDouble()))
                .willReturn(updated);

        mockMvc.perform(patch("/routines/1/exercises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateRoutineExerciseRequest("인클라인 벤치프레스", 3, 10, 55.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseName").value("인클라인 벤치프레스"))
                .andExpect(jsonPath("$.targetSets").value(3));
    }

    @Test
    void 없는_루틴_수정시_404_반환() throws Exception {
        given(routineService.updateExercise(anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyDouble()))
                .willThrow(new NoSuchElementException("루틴을 찾을 수 없습니다."));

        mockMvc.perform(patch("/routines/99/exercises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateRoutineExerciseRequest("벤치프레스", 4, 8, 60.0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.presentation.RoutineControllerTest" 2>&1 | tail -20
```

Expected: 컴파일 오류 (RoutineController 없음)

- [ ] **Step 3: CreateAiRoutineRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/CreateAiRoutineRequest.java
package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.domain.SplitType;

public record CreateAiRoutineRequest(Long memberId, int daysPerWeek, SplitType splitType) {}
```

- [ ] **Step 4: CreateManualRoutineRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/CreateManualRoutineRequest.java
package com.ssafy.manager.routine.presentation;

import java.util.List;

public record CreateManualRoutineRequest(
        Long memberId,
        String name,
        int daysPerWeek,
        List<ExerciseItem> exercises
) {
    public record ExerciseItem(
            String dayLabel,
            String exerciseName,
            int targetSets,
            int targetReps,
            double targetWeightKg,
            int orderIndex
    ) {}
}
```

- [ ] **Step 5: UpdateRoutineExerciseRequest.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/UpdateRoutineExerciseRequest.java
package com.ssafy.manager.routine.presentation;

public record UpdateRoutineExerciseRequest(
        String exerciseName,
        int targetSets,
        int targetReps,
        double targetWeightKg
) {}
```

- [ ] **Step 6: RoutineResponse.java 생성**

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
            int orderIndex
    ) {
        public static ExerciseResponse from(RoutineResult.ExerciseResult r) {
            return new ExerciseResponse(r.id(), r.dayLabel(), r.exerciseName(),
                    r.targetSets(), r.targetReps(), r.targetWeightKg(), r.orderIndex());
        }
    }

    public static RoutineResponse from(RoutineResult result) {
        return new RoutineResponse(
                result.routineId(),
                result.name(),
                result.daysPerWeek(),
                result.aiGenerated(),
                result.exercises().stream().map(ExerciseResponse::from).toList(),
                result.aiComment()
        );
    }
}
```

- [ ] **Step 7: RoutineController.java 생성**

```java
// backend/src/main/java/com/ssafy/manager/routine/presentation/RoutineController.java
package com.ssafy.manager.routine.presentation;

import com.ssafy.manager.routine.application.ExerciseInput;
import com.ssafy.manager.routine.application.RoutineResult;
import com.ssafy.manager.routine.application.RoutineService;
import com.ssafy.manager.routine.domain.SplitType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @GetMapping("/split-options")
    public List<SplitOptionResponse> getSplitOptions(@RequestParam int daysPerWeek) {
        return SplitType.findByDaysPerWeek(daysPerWeek).stream()
                .map(s -> new SplitOptionResponse(s.name(), s.getLabel()))
                .toList();
    }

    @PostMapping("/ai")
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse createAi(@RequestBody CreateAiRoutineRequest request) {
        RoutineResult result = routineService.createAi(
                request.memberId(), request.daysPerWeek(), request.splitType());
        return RoutineResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineResponse createManual(@RequestBody CreateManualRoutineRequest request) {
        List<ExerciseInput> inputs = request.exercises().stream()
                .map(e -> new ExerciseInput(e.dayLabel(), e.exerciseName(),
                        e.targetSets(), e.targetReps(), e.targetWeightKg(), e.orderIndex()))
                .toList();
        RoutineResult result = routineService.createManual(
                request.memberId(), request.name(), request.daysPerWeek(), inputs);
        return RoutineResponse.from(result);
    }

    @PatchMapping("/{routineId}/exercises/{exerciseId}")
    public RoutineResponse.ExerciseResponse updateExercise(
            @PathVariable Long routineId,
            @PathVariable Long exerciseId,
            @RequestBody UpdateRoutineExerciseRequest request) {
        RoutineResult.ExerciseResult result = routineService.updateExercise(
                routineId, exerciseId,
                request.exerciseName(), request.targetSets(),
                request.targetReps(), request.targetWeightKg());
        return RoutineResponse.ExerciseResponse.from(result);
    }

    private record SplitOptionResponse(String splitType, String label) {}
}
```

- [ ] **Step 8: 테스트 실행 — 통과 확인**

```bash
cd backend && ./gradlew test --tests "com.ssafy.manager.routine.presentation.RoutineControllerTest"
```

Expected: 5 tests passed

- [ ] **Step 9: 전체 테스트 실행**

```bash
cd backend && ./gradlew test
```

Expected: All tests passed (기존 테스트 포함)

- [ ] **Step 10: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/routine/presentation/ \
        backend/src/test/java/com/ssafy/manager/routine/presentation/
git commit -m "feat(routine): RoutineController 구현 (AI 생성/수동 등록/운동 수정)"
```

---

## 완료 체크리스트

- [ ] FastAPI `POST /ai/routine/generate` — mock 모드로 정상 응답
- [ ] Spring `GET /routines/split-options?daysPerWeek=4` — 2개 옵션 반환
- [ ] Spring `POST /routines/ai` — FastAPI 호출 후 루틴 저장, 201 반환
- [ ] Spring `POST /routines` — 직접 등록, FastAPI 미호출, 201 반환
- [ ] Spring `PATCH /routines/{id}/exercises/{id}` — 운동 수정, 200 반환
- [ ] 없는 회원/루틴/운동 → 404, FastAPI 장애 → 503
- [ ] 전체 기존 테스트 통과
