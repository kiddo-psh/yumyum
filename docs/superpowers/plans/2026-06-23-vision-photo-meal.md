# Vision AI 사진 식단 분석 구현 플랜

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사진을 찍으면 Claude Vision이 음식을 감지하고 영양소를 추정해 Meal로 저장하는 기능을 구현한다.

**Architecture:** Vue가 이미지를 base64로 변환해 Spring에 JSON으로 전송 → Spring이 FastAPI에 프록시 → FastAPI가 Claude Vision 호출 → 분석 결과를 Vue에 반환 → 사용자 확인 후 Spring이 Meal 저장. MealItem의 기존 영양소 필드(calories·protein·fat·carbs)를 재사용하고 foodCode를 null로 저장한다(스키마 변경 없음).

**Tech Stack:** Python 3.11 FastAPI·httpx·pytest, Java 21 Spring Boot 3·RestClient·JUnit 5, Vue 3 composition API·FileReader API

## Global Constraints

- FastAPI dev 환경(`ENV=dev`)에서는 Claude API 호출 없이 mock 응답 반환
- 모든 Claude 호출은 `claude_service.py`를 통해서만 수행 — 엔드포인트에서 직접 호출 금지
- Spring Controller는 항상 `ResponseEntity`로 응답 — `@ResponseStatus` 직접 반환 금지
- Spring 서비스 메서드는 `application` 레이어 Result 타입을 반환하고, Controller에서 presentation DTO로 변환
- `/ai/**` URL은 Spring → FastAPI 내부 호출 전용 — Vue가 FastAPI를 직접 호출하지 않음
- FastAPI vision 모델: `claude-opus-4-5-20251101` (CLAUDE.md: RAG·심화 기능은 opus 사용)
- 이미지 최대 크기: 10MB (Vue에서 클라이언트 사이드 검증)
- 커밋 메시지 형식: `feat(meal): 설명`, `test(meal): 설명`, `docs: 설명`
- 브랜치: 현재 `feature/weight-input-simplify`에서 계속 작업

---

## 파일 구조 요약

**신규 생성:**
- `ai/tests/test_claude_vision.py`
- `ai/tests/test_ai_meal_photo.py`
- `backend/.../nutrition/application/PhotoMealCommand.java`
- `backend/.../nutrition/application/PhotoMealItemCommand.java`
- `backend/.../nutrition/application/AiMealPhotoAnalyzeResult.java`
- `backend/.../nutrition/infrastructure/client/AiMealPhotoClientRequest.java`
- `backend/.../nutrition/infrastructure/client/AiMealPhotoClientResponse.java`
- `backend/.../nutrition/presentation/dto/PhotoAnalysisResponse.java`
- `backend/.../nutrition/presentation/dto/PhotoMealRequest.java`
- `frontend/src/views/MealPhotoView.vue`
- `frontend/src/api/meal.js`
- `docs/adr/2026-06-23-vision-ai-photo-meal.md`

**수정:**
- `ai/app/services/claude_service.py` — `call_claude_vision()` 추가
- `ai/app/schemas/meal.py` — `PhotoAnalyzeRequest`, `DetectedItem`, `PhotoAnalyzeResponse` 추가
- `ai/app/routers/ai_meal.py` — `POST /ai/meal/analyze-photo` 추가
- `backend/.../nutrition/domain/MealItem.java` — `fromAiEstimate()` 팩토리 메서드 추가
- `backend/.../nutrition/domain/Meal.java` — `addAiItem()` 메서드 추가
- `backend/.../nutrition/infrastructure/client/AiMealClient.java` — `analyzePhoto()` 추가
- `backend/.../nutrition/application/MealService.java` — `recordFromPhoto()` 추가
- `backend/.../nutrition/application/AiMealService.java` — `analyzePhoto()` 추가
- `backend/.../nutrition/presentation/AiMealController.java` — `POST /meals/photo/analyze` 추가
- `backend/.../nutrition/presentation/MealController.java` — `POST /meals/photo` 추가
- `backend/.../global/config/RestClientConfig.java` — `aiMealRestClient` read timeout 30s로 변경
- `frontend/src/router/index.js` — meal-photo 라우트를 MealPhotoView로 교체

---

### Task 1: FastAPI — call_claude_vision() 구현

**Files:**
- Modify: `ai/app/services/claude_service.py`
- Create: `ai/tests/test_claude_vision.py`

**Interfaces:**
- Produces: `async def call_claude_vision(image_base64: str, media_type: str, prompt: str, model: str | None = None, max_tokens: int = 800) -> str`

- [ ] **Step 1: 테스트 파일 생성 — mock 모드 응답 검증**

`ai/tests/test_claude_vision.py`:
```python
import os
os.environ.setdefault("ENV", "dev")

import pytest
from app.services.claude_service import call_claude_vision


@pytest.mark.asyncio
async def test_vision_mock_모드에서_json_문자열_반환():
    result = await call_claude_vision(
        image_base64="dGVzdA==",
        media_type="image/jpeg",
        prompt="음식을 감지하세요",
    )
    assert isinstance(result, str)
    assert len(result) > 0


@pytest.mark.asyncio
async def test_vision_mock_응답에_detected_items_포함():
    import json
    result = await call_claude_vision(
        image_base64="dGVzdA==",
        media_type="image/jpeg",
        prompt="음식을 감지하세요",
    )
    data = json.loads(result)
    assert "detected_items" in data
    assert isinstance(data["detected_items"], list)
    assert len(data["detected_items"]) > 0


@pytest.mark.asyncio
async def test_vision_mock_각_아이템에_필수_필드_존재():
    import json
    result = await call_claude_vision(
        image_base64="dGVzdA==",
        media_type="image/jpeg",
        prompt="음식을 감지하세요",
    )
    data = json.loads(result)
    item = data["detected_items"][0]
    for field in ["name", "estimated_grams", "kcal", "protein_g", "carb_g", "fat_g"]:
        assert field in item, f"필드 누락: {field}"
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd ai
pytest tests/test_claude_vision.py -v
```

Expected: `FAILED` — `cannot import name 'call_claude_vision'`

- [ ] **Step 3: claude_service.py에 call_claude_vision() 추가**

`ai/app/services/claude_service.py` 파일 끝에 추가:

```python
async def call_claude_vision(
    image_base64: str,
    media_type: str,
    prompt: str,
    model: str | None = None,
    max_tokens: int = 800,
) -> str:
    """
    Claude Vision API 호출. 이미지 + 텍스트 프롬프트를 받아 분석 결과를 문자열로 반환.
    dev 환경에서는 mock JSON 응답 반환.
    """
    if settings.env == "dev":
        return _mock_vision_response()

    return await _call_gms_vision(
        image_base64=image_base64,
        media_type=media_type,
        prompt=prompt,
        model=model or "claude-opus-4-5-20251101",
        max_tokens=max_tokens,
    )


async def _call_gms_vision(
    image_base64: str, media_type: str, prompt: str, model: str, max_tokens: int
) -> str:
    url = f"{settings.gms_base_url}/v1/messages"
    headers = {
        "Content-Type": "application/json",
        "x-api-key": settings.gms_api_key,
        "anthropic-version": settings.anthropic_version,
    }
    payload = {
        "model": model,
        "max_tokens": max_tokens,
        "messages": [{
            "role": "user",
            "content": [
                {
                    "type": "image",
                    "source": {
                        "type": "base64",
                        "media_type": media_type,
                        "data": image_base64,
                    },
                },
                {"type": "text", "text": prompt},
            ],
        }],
    }

    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.post(url, headers=headers, json=payload)
        response.raise_for_status()

    data = response.json()
    return data["content"][0]["text"]


def _mock_vision_response() -> str:
    return json.dumps({
        "detected_items": [
            {
                "name": "닭가슴살",
                "estimated_grams": 150.0,
                "kcal": 165.0,
                "protein_g": 31.0,
                "carb_g": 0.0,
                "fat_g": 3.6,
            },
            {
                "name": "현미밥",
                "estimated_grams": 200.0,
                "kcal": 278.0,
                "protein_g": 5.6,
                "carb_g": 58.0,
                "fat_g": 1.6,
            },
        ],
        "total_kcal": 443.0,
        "ai_comment": "[MOCK] 고단백 균형 식단이네요! 단백질 섭취가 훌륭합니다.",
    }, ensure_ascii=False)
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd ai
pytest tests/test_claude_vision.py -v
```

Expected: 3 tests PASSED

- [ ] **Step 5: 커밋**

```bash
git add ai/app/services/claude_service.py ai/tests/test_claude_vision.py
git commit -m "feat(chat): Vision AI call_claude_vision() 구현 + mock 응답"
```

---

### Task 2: FastAPI — 사진 분석 스키마 + 엔드포인트

**Files:**
- Modify: `ai/app/schemas/meal.py`
- Modify: `ai/app/routers/ai_meal.py`
- Create: `ai/tests/test_ai_meal_photo.py`

**Interfaces:**
- Consumes: `call_claude_vision(image_base64, media_type, prompt)` from Task 1
- Produces: `POST /ai/meal/analyze-photo` → `PhotoAnalyzeResponse`

- [ ] **Step 1: 테스트 파일 생성**

`ai/tests/test_ai_meal_photo.py`:
```python
import os
os.environ.setdefault("ENV", "dev")

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_REQUEST = {
    "image_base64": "dGVzdA==",
    "media_type": "image/jpeg",
    "meal_type": "LUNCH",
}


def test_사진_분석_200_반환():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    assert response.status_code == 200


def test_사진_분석_응답_구조_검증():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    data = response.json()
    assert "detected_items" in data
    assert "total_kcal" in data
    assert "ai_comment" in data
    assert isinstance(data["detected_items"], list)


def test_detected_item_필드_검증():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    items = response.json()["detected_items"]
    assert len(items) > 0
    item = items[0]
    for field in ["name", "estimated_grams", "kcal", "protein_g", "carb_g", "fat_g"]:
        assert field in item


def test_total_kcal_아이템_합산과_일치():
    response = client.post("/ai/meal/analyze-photo", json=VALID_REQUEST)
    data = response.json()
    items_sum = sum(i["kcal"] for i in data["detected_items"])
    assert abs(data["total_kcal"] - items_sum) < 1.0


def test_빈_base64는_mock_모드에서도_정상_처리():
    """dev 환경에서는 base64 값과 무관하게 mock 응답 반환"""
    response = client.post("/ai/meal/analyze-photo", json={
        "image_base64": "",
        "media_type": "image/jpeg",
        "meal_type": "BREAKFAST",
    })
    assert response.status_code == 200
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd ai
pytest tests/test_ai_meal_photo.py -v
```

Expected: `FAILED` — 404 Not Found (엔드포인트 미존재)

- [ ] **Step 3: schemas/meal.py에 Photo 스키마 추가**

`ai/app/schemas/meal.py` 파일 끝에 추가:

```python
class PhotoAnalyzeRequest(BaseModel):
    image_base64: str
    media_type: str   # "image/jpeg" | "image/png" | "image/webp"
    meal_type: str    # "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK"


class DetectedItem(BaseModel):
    name: str
    estimated_grams: float
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float


class PhotoAnalyzeResponse(BaseModel):
    detected_items: List[DetectedItem]
    total_kcal: float
    ai_comment: str
```

- [ ] **Step 4: ai_meal.py에 엔드포인트 추가**

`ai/app/routers/ai_meal.py` 상단 import에 추가:
```python
from app.schemas.meal import (
    LastRecommendRequest, LastRecommendResponse, MealRecommendation,
    DietAnalyzeRequest, DietAnalyzeResponse,
    PhotoAnalyzeRequest, DetectedItem, PhotoAnalyzeResponse,
)
from app.services.claude_service import call_claude, call_claude_vision
```

파일 끝에 엔드포인트 추가:
```python
@router.post("/analyze-photo", response_model=PhotoAnalyzeResponse)
async def analyze_photo(req: PhotoAnalyzeRequest):
    """
    Vision AI 사진 식단 분석.
    이미지 base64를 받아 Claude Vision으로 음식 감지 + 영양소 추정.
    Spring이 multipart → base64 변환 후 호출.
    """
    prompt = (
        f"이 사진에 있는 음식을 모두 감지하고 영양소를 추정해주세요. 식사 유형: {req.meal_type}\n\n"
        "아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이):\n"
        '{"detected_items": [{"name": "음식명(한국어)", "estimated_grams": 숫자, '
        '"kcal": 숫자, "protein_g": 숫자, "carb_g": 숫자, "fat_g": 숫자}], '
        '"total_kcal": 합계숫자, "ai_comment": "한 문장 한국어 코멘트"}\n\n'
        "음식이 감지되지 않으면 detected_items를 빈 배열로 반환하세요."
    )

    try:
        raw = await call_claude_vision(
            image_base64=req.image_base64,
            media_type=req.media_type,
            prompt=prompt,
            max_tokens=800,
        )
        cleaned = raw.strip()
        if "```" in cleaned:
            parts = cleaned.split("```")
            cleaned = parts[1] if len(parts) > 1 else cleaned
            if cleaned.startswith("json"):
                cleaned = cleaned[4:]
        data = json.loads(cleaned)
    except Exception as e:
        print(f"[analyze_photo] vision 분석 실패 — {type(e).__name__}: {e}")
        data = {
            "detected_items": [],
            "total_kcal": 0.0,
            "ai_comment": "음식을 인식하지 못했어요. 다시 촬영해보세요.",
        }

    items = [DetectedItem(**item) for item in data.get("detected_items", [])]
    total_kcal = sum(i.kcal for i in items)

    return PhotoAnalyzeResponse(
        detected_items=items,
        total_kcal=total_kcal,
        ai_comment=data.get("ai_comment", ""),
    )
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
cd ai
pytest tests/test_ai_meal_photo.py -v
```

Expected: 5 tests PASSED

- [ ] **Step 6: 커밋**

```bash
git add ai/app/schemas/meal.py ai/app/routers/ai_meal.py ai/tests/test_ai_meal_photo.py
git commit -m "feat(meal): POST /ai/meal/analyze-photo Vision 엔드포인트 구현"
```

---

### Task 3: Spring — MealItem·Meal AI 팩토리 메서드

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/domain/MealItem.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/domain/Meal.java`

**Interfaces:**
- Produces:
  - `MealItem.fromAiEstimate(String itemName, double grams, double kcal, double protein, double carb, double fat): MealItem`
  - `Meal.addAiItem(String name, double grams, double kcal, double protein, double carb, double fat): MealItem`

> **참고:** MealItem의 기존 필드(`foodCode`, `foodName`, `calories`, `carbs`, `protein`, `fat`, `fiber`)는 이미 nullable이므로 스키마 변경 없이 재사용한다. AI 아이템은 `foodCode=null`, `fiber=0.0`으로 저장.

- [ ] **Step 1: MealItem에 fromAiEstimate() 추가**

`MealItem.java`의 기존 `from(Food food, double amountGrams)` 아래에 추가:

```java
public static MealItem fromAiEstimate(String itemName, double amountGrams,
                                       double kcal, double protein,
                                       double carb, double fat) {
    MealItem item = new MealItem(amountGrams, kcal, carb, protein, fat, 0.0);
    item.foodCode = null;
    item.foodName = itemName;
    return item;
}
```

- [ ] **Step 2: Meal에 addAiItem() 추가**

`Meal.java`의 기존 `addItem(Food food, double amountGrams)` 아래에 추가:

```java
public MealItem addAiItem(String name, double amountGrams,
                           double kcal, double protein,
                           double carb, double fat) {
    MealItem item = MealItem.fromAiEstimate(name, amountGrams, kcal, protein, carb, fat);
    item.bindTo(this);
    items.add(item);
    return item;
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd backend
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/nutrition/domain/MealItem.java
git add backend/src/main/java/com/ssafy/manager/nutrition/domain/Meal.java
git commit -m "feat(meal): MealItem.fromAiEstimate() + Meal.addAiItem() 추가"
```

---

### Task 4: Spring — FastAPI 클라이언트 DTOs + AiMealClient 확장

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealPhotoClientRequest.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealPhotoClientResponse.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealClient.java`
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java`

**Interfaces:**
- Consumes: FastAPI `POST /ai/meal/analyze-photo` 응답 (snake_case JSON)
- Produces: `AiMealClient.analyzePhoto(AiMealPhotoClientRequest): AiMealPhotoClientResponse`

- [ ] **Step 1: AiMealPhotoClientRequest 생성**

`backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealPhotoClientRequest.java`:

```java
package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiMealPhotoClientRequest(
        @JsonProperty("image_base64") String imageBase64,
        @JsonProperty("media_type")   String mediaType,
        @JsonProperty("meal_type")    String mealType
) {}
```

- [ ] **Step 2: AiMealPhotoClientResponse 생성**

`backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealPhotoClientResponse.java`:

```java
package com.ssafy.manager.nutrition.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiMealPhotoClientResponse(
        @JsonProperty("detected_items") List<DetectedItemDto> detectedItems,
        @JsonProperty("total_kcal")     double totalKcal,
        @JsonProperty("ai_comment")     String aiComment
) {
    public record DetectedItemDto(
            String name,
            @JsonProperty("estimated_grams") double estimatedGrams,
            double kcal,
            @JsonProperty("protein_g") double proteinG,
            @JsonProperty("carb_g")    double carbG,
            @JsonProperty("fat_g")     double fatG
    ) {}
}
```

- [ ] **Step 3: AiMealClient에 analyzePhoto() 추가**

`AiMealClient.java`에 메서드 추가:

```java
public AiMealPhotoClientResponse analyzePhoto(AiMealPhotoClientRequest request) {
    return aiMealRestClient.post()
            .uri("/ai/meal/analyze-photo")
            .body(request)
            .retrieve()
            .body(AiMealPhotoClientResponse.class);
}
```

- [ ] **Step 4: RestClientConfig — aiMealRestClient read timeout 30s로 변경**

Vision API 호출은 최대 20초 소요 가능. `aiMealRestClient` 빈의 read timeout을 30s로 변경:

```java
@Bean
RestClient aiMealRestClient(
        @Value("${ai.fastapi.url}") String baseUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(30000);  // 15000 → 30000 (Vision AI 응답 대기)
    return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("Content-Type", "application/json")
            .build();
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd backend
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealPhotoClientRequest.java
git add backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealPhotoClientResponse.java
git add backend/src/main/java/com/ssafy/manager/nutrition/infrastructure/client/AiMealClient.java
git add backend/src/main/java/com/ssafy/manager/global/config/RestClientConfig.java
git commit -m "feat(meal): AiMealClient.analyzePhoto() + 클라이언트 DTOs 추가"
```

---

### Task 5: Spring — 서비스·컨트롤러 엔드포인트 2개

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/application/PhotoMealItemCommand.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/application/PhotoMealCommand.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealPhotoAnalyzeResult.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/application/MealService.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealService.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/PhotoAnalysisResponse.java`
- Create: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/PhotoMealRequest.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/AiMealController.java`
- Modify: `backend/src/main/java/com/ssafy/manager/nutrition/presentation/MealController.java`

**Interfaces:**
- Consumes (Tasks 3·4): `Meal.addAiItem()`, `AiMealClient.analyzePhoto()`
- Produces:
  - `POST /meals/photo/analyze` → `PhotoAnalysisResponse`
  - `POST /meals/photo` → `MealResponse`

- [ ] **Step 1: Application layer command/result 레코드 생성**

`PhotoMealItemCommand.java`:
```java
package com.ssafy.manager.nutrition.application;

public record PhotoMealItemCommand(
        String name,
        double estimatedGrams,
        double kcal,
        double proteinG,
        double carbG,
        double fatG
) {}
```

`PhotoMealCommand.java`:
```java
package com.ssafy.manager.nutrition.application;

import com.ssafy.manager.nutrition.domain.MealType;

import java.util.List;

public record PhotoMealCommand(
        Long memberId,
        MealType mealType,
        List<PhotoMealItemCommand> items
) {}
```

`AiMealPhotoAnalyzeResult.java`:
```java
package com.ssafy.manager.nutrition.application;

import java.util.List;

public record AiMealPhotoAnalyzeResult(
        List<DetectedItemResult> detectedItems,
        double totalKcal,
        String aiComment
) {
    public record DetectedItemResult(
            String name,
            double estimatedGrams,
            double kcal,
            double proteinG,
            double carbG,
            double fatG
    ) {}
}
```

- [ ] **Step 2: MealService에 recordFromPhoto() 추가**

`MealService.java`에 메서드 추가 (기존 `record()` 아래):

```java
@Transactional
public Meal recordFromPhoto(PhotoMealCommand command, LocalDateTime recordedAt) {
    LocalDate effectiveDate = effectiveDateOf(recordedAt);
    MealType resolvedType = command.mealType() != null ? command.mealType() : inferMealType(recordedAt);

    Meal meal = new Meal(command.memberId(), resolvedType, recordedAt.toLocalDate(), effectiveDate);
    for (PhotoMealItemCommand item : command.items()) {
        meal.addAiItem(item.name(), item.estimatedGrams(),
                item.kcal(), item.proteinG(), item.carbG(), item.fatG());
    }
    mealRepository.save(meal);

    double totalCalories = mealItemRepository.sumCaloriesByMemberIdAndEffectiveDate(
            command.memberId(), effectiveDate);
    dailyGoalRepository.findByMemberIdAndDate(command.memberId(), effectiveDate)
            .ifPresent(goal -> updateGoalAndStreak(goal, totalCalories, command.memberId(), effectiveDate));

    return meal;
}
```

- [ ] **Step 3: AiMealService에 analyzePhoto() 추가**

현재 `AiMealService.java`를 먼저 확인한 뒤 아래 메서드를 추가한다.
(`AiMealService.java` 위치: `backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealService.java`)

```java
// 파일 상단 import에 추가:
// import com.ssafy.manager.nutrition.infrastructure.client.AiMealPhotoClientRequest;
// import com.ssafy.manager.nutrition.infrastructure.client.AiMealPhotoClientResponse;

public AiMealPhotoAnalyzeResult analyzePhoto(String imageBase64, String mediaType, String mealType) {
    AiMealPhotoClientRequest request = new AiMealPhotoClientRequest(imageBase64, mediaType, mealType);
    AiMealPhotoClientResponse response = aiMealClient.analyzePhoto(request);

    List<AiMealPhotoAnalyzeResult.DetectedItemResult> items = response.detectedItems().stream()
            .map(d -> new AiMealPhotoAnalyzeResult.DetectedItemResult(
                    d.name(), d.estimatedGrams(), d.kcal(), d.proteinG(), d.carbG(), d.fatG()))
            .toList();

    return new AiMealPhotoAnalyzeResult(items, response.totalKcal(), response.aiComment());
}
```

- [ ] **Step 4: 프레젠테이션 DTOs 생성**

`PhotoAnalysisResponse.java`:
```java
package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.AiMealPhotoAnalyzeResult;

import java.util.List;

public record PhotoAnalysisResponse(
        List<DetectedItemDto> detectedItems,
        double totalKcal,
        String aiComment
) {
    public record DetectedItemDto(
            String name,
            double estimatedGrams,
            double kcal,
            double proteinG,
            double carbG,
            double fatG
    ) {}

    public static PhotoAnalysisResponse from(AiMealPhotoAnalyzeResult result) {
        List<DetectedItemDto> items = result.detectedItems().stream()
                .map(d -> new DetectedItemDto(
                        d.name(), d.estimatedGrams(), d.kcal(),
                        d.proteinG(), d.carbG(), d.fatG()))
                .toList();
        return new PhotoAnalysisResponse(items, result.totalKcal(), result.aiComment());
    }
}
```

`PhotoMealRequest.java`:
```java
package com.ssafy.manager.nutrition.presentation.dto;

import com.ssafy.manager.nutrition.application.PhotoMealCommand;
import com.ssafy.manager.nutrition.application.PhotoMealItemCommand;
import com.ssafy.manager.nutrition.domain.MealType;

import java.util.List;

public record PhotoMealRequest(
        MealType mealType,
        List<PhotoMealItemRequest> items
) {
    public record PhotoMealItemRequest(
            String name,
            double estimatedGrams,
            double kcal,
            double proteinG,
            double carbG,
            double fatG
    ) {}

    public PhotoMealCommand toCommand(Long memberId) {
        List<PhotoMealItemCommand> cmds = items.stream()
                .map(i -> new PhotoMealItemCommand(
                        i.name(), i.estimatedGrams(), i.kcal(),
                        i.proteinG(), i.carbG(), i.fatG()))
                .toList();
        return new PhotoMealCommand(memberId, mealType, cmds);
    }
}
```

- [ ] **Step 5: presentation DTO PhotoAnalysisRequest 생성 + AiMealController에 POST /meals/photo/analyze 추가**

Vue가 JSON body로 전송하므로 `@RequestBody` 사용. 먼저 request DTO 생성:

`PhotoAnalysisRequest.java` (`presentation/dto/` 패키지):
```java
package com.ssafy.manager.nutrition.presentation.dto;

public record PhotoAnalysisRequest(
        String imageBase64,
        String mediaType,
        String mealType
) {}
```

`AiMealController.java`에 추가:
```java
// 기존 import에 추가:
// import com.ssafy.manager.nutrition.presentation.dto.PhotoAnalysisRequest;
// import com.ssafy.manager.nutrition.presentation.dto.PhotoAnalysisResponse;

@PostMapping("/photo/analyze")
public ResponseEntity<PhotoAnalysisResponse> analyzePhoto(
        @AuthenticationPrincipal Long memberId,
        @RequestBody PhotoAnalysisRequest request
) {
    AiMealPhotoAnalyzeResult result = aiMealService.analyzePhoto(
            request.imageBase64(), request.mediaType(), request.mealType());
    return ResponseEntity.ok(PhotoAnalysisResponse.from(result));
}
```

- [ ] **Step 6: MealController에 POST /meals/photo 추가**

`MealController.java`에 추가:
```java
// 기존 import에 추가:
// import com.ssafy.manager.nutrition.presentation.dto.PhotoMealRequest;

@PostMapping("/photo")
public ResponseEntity<MealResponse> recordFromPhoto(
        @AuthenticationPrincipal Long memberId,
        @RequestBody PhotoMealRequest request,
        UriComponentsBuilder uriBuilder
) {
    LocalDateTime now = LocalDateTime.now();
    Meal meal = mealService.recordFromPhoto(request.toCommand(memberId), now);
    return ResponseEntity.created(
            uriBuilder.path("/meals/{id}").buildAndExpand(meal.getId()).toUri()
    ).body(MealResponse.from(meal));
}
```

- [ ] **Step 7: 빌드 확인**

```bash
cd backend
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/nutrition/application/PhotoMealItemCommand.java
git add backend/src/main/java/com/ssafy/manager/nutrition/application/PhotoMealCommand.java
git add backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealPhotoAnalyzeResult.java
git add backend/src/main/java/com/ssafy/manager/nutrition/application/MealService.java
git add backend/src/main/java/com/ssafy/manager/nutrition/application/AiMealService.java
git add backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/PhotoAnalysisRequest.java
git add backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/PhotoAnalysisResponse.java
git add backend/src/main/java/com/ssafy/manager/nutrition/presentation/dto/PhotoMealRequest.java
git add backend/src/main/java/com/ssafy/manager/nutrition/presentation/AiMealController.java
git add backend/src/main/java/com/ssafy/manager/nutrition/presentation/MealController.java
git commit -m "feat(meal): POST /meals/photo/analyze + POST /meals/photo 엔드포인트 구현"
```

---

### Task 6: Vue — MealPhotoView + 라우터 + API 함수

**Files:**
- Create: `frontend/src/api/meal.js`
- Create: `frontend/src/views/MealPhotoView.vue`
- Modify: `frontend/src/router/index.js`

**Interfaces:**
- Consumes: `POST /meals/photo/analyze`, `POST /meals/photo` (Spring endpoints from Task 5)

> **Note:** `apiClient.post()`는 JSON body만 지원한다. 이미지는 Vue에서 FileReader로 base64 변환 후 JSON으로 전송한다. multipart 불필요.

- [ ] **Step 1: frontend/src/api/meal.js 생성**

```js
import { apiClient, request } from '@/services/apiClient';

/**
 * 이미지 File 객체를 base64 문자열로 변환한다 (data:... prefix 제거).
 */
function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve(e.target.result.split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/**
 * 사진 분석 (DB 저장 없음). 감지된 음식 목록 + 총 칼로리 + AI 코멘트 반환.
 * @param {File} imageFile
 * @param {string} mealType - "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK"
 */
export async function analyzePhoto(imageFile, mealType = 'LUNCH') {
  const imageBase64 = await fileToBase64(imageFile);
  const mediaType = imageFile.type || 'image/jpeg';
  return apiClient.post('/meals/photo/analyze', { imageBase64, mediaType, mealType });
}

/**
 * AI 분석 결과로 Meal 저장.
 * @param {string} mealType
 * @param {Array<{name, estimatedGrams, kcal, proteinG, carbG, fatG}>} items
 */
export function recordPhotoMeal(mealType, items) {
  return apiClient.post('/meals/photo', { mealType, items });
}
```

- [ ] **Step 2: MealPhotoView.vue 생성**

`frontend/src/views/MealPhotoView.vue`:

```vue
<template>
  <div class="max-w-2xl mx-auto">
    <div class="flex items-center gap-3 mb-8">
      <RouterLink to="/" class="w-10 h-10 flex items-center justify-center neo-brutal-border rounded-full hover:bg-surface transition-colors">
        <span class="material-symbols-outlined">arrow_back</span>
      </RouterLink>
      <h1 class="text-headline-lg">사진으로 기록</h1>
    </div>

    <!-- idle: 사진 선택 -->
    <div v-if="phase === 'idle'" class="bg-white neo-brutal-border rounded-xl p-8 text-center neo-brutal-card-hover">
      <span class="material-symbols-outlined text-[80px] text-primary mb-4 block" style="font-variation-settings:'FILL' 1;">camera_enhance</span>
      <p class="text-body-lg text-on-surface-variant mb-6">식사 사진을 찍거나 갤러리에서 선택하세요</p>
      <p v-if="sizeError" class="text-danger text-sm font-bold mb-4">{{ sizeError }}</p>
      <label class="cursor-pointer inline-block bg-primary text-white px-8 py-3 neo-brutal-border rounded-lg font-bold hover:-translate-y-1 transition-transform">
        <span class="material-symbols-outlined align-middle mr-2">add_a_photo</span>사진 선택
        <input
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden"
          @change="onFileSelected"
        />
      </label>
    </div>

    <!-- analyzing: 분석 중 -->
    <div v-else-if="phase === 'analyzing'" class="bg-white neo-brutal-border rounded-xl p-12 text-center">
      <span class="material-symbols-outlined text-[60px] text-primary animate-spin block mb-4">progress_activity</span>
      <p class="text-headline-md">AI가 음식을 분석하는 중...</p>
      <p class="text-body-md text-on-surface-variant mt-2">잠시만 기다려주세요</p>
    </div>

    <!-- result: 분석 완료 -->
    <div v-else-if="phase === 'result'">
      <!-- 미리보기 이미지 -->
      <div class="mb-6 neo-brutal-border rounded-xl overflow-hidden">
        <img :src="previewUrl" alt="분석된 사진" class="w-full max-h-64 object-cover" />
      </div>

      <!-- AI 코멘트 -->
      <div class="bg-nyam-mint neo-brutal-border rounded-xl p-5 mb-6 flex items-start gap-3">
        <span class="material-symbols-outlined text-primary flex-shrink-0" style="font-variation-settings:'FILL' 1;">smart_toy</span>
        <p class="text-body-md">{{ analysisResult.aiComment }}</p>
      </div>

      <!-- 감지된 음식 없음 -->
      <div v-if="analysisResult.detectedItems.length === 0" class="bg-white neo-brutal-border rounded-xl p-8 text-center mb-6">
        <p class="text-on-surface-variant">음식을 감지하지 못했어요.</p>
        <button class="mt-4 px-6 py-2 neo-brutal-border rounded-lg font-bold hover:-translate-y-1 transition-transform" @click="reset">다시 촬영</button>
      </div>

      <!-- 감지된 음식 목록 -->
      <div v-else>
        <div class="flex items-center justify-between mb-4">
          <h2 class="font-bold text-lg">감지된 음식</h2>
          <span class="text-primary font-bold">총 {{ Math.round(analysisResult.totalKcal) }} kcal</span>
        </div>

        <div class="space-y-3 mb-8">
          <div
            v-for="item in analysisResult.detectedItems"
            :key="item.name"
            class="bg-white neo-brutal-border rounded-xl p-4 flex items-center justify-between"
          >
            <div>
              <p class="font-bold">{{ item.name }}</p>
              <p class="text-sm text-on-surface-variant">약 {{ item.estimatedGrams }}g</p>
            </div>
            <div class="text-right">
              <p class="font-bold text-primary">{{ Math.round(item.kcal) }} kcal</p>
              <p class="text-xs text-on-surface-variant">
                단{{ Math.round(item.proteinG) }}g 탄{{ Math.round(item.carbG) }}g 지{{ Math.round(item.fatG) }}g
              </p>
            </div>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            class="flex-1 py-3 neo-brutal-border rounded-xl font-bold hover:-translate-y-1 transition-transform"
            @click="reset"
          >
            다시 촬영
          </button>
          <button
            class="flex-1 py-3 bg-primary text-white neo-brutal-border rounded-xl font-bold disabled:opacity-50 hover:-translate-y-1 transition-transform"
            :disabled="phase === 'saving'"
            @click="saveMeal"
          >
            {{ phase === 'saving' ? '저장 중...' : '식단에 추가' }}
          </button>
        </div>
      </div>
    </div>

    <!-- error -->
    <div v-else-if="phase === 'error'" class="bg-white neo-brutal-border rounded-xl p-8 text-center">
      <span class="material-symbols-outlined text-[60px] text-danger block mb-4">error</span>
      <p class="text-headline-md mb-2">분석 실패</p>
      <p class="text-body-md text-on-surface-variant mb-6">{{ errorMessage }}</p>
      <button class="px-8 py-3 neo-brutal-border rounded-xl font-bold hover:-translate-y-1 transition-transform" @click="reset">다시 시도</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { analyzePhoto, recordPhotoMeal } from '@/api/meal'

const router = useRouter()

const phase = ref('idle')        // 'idle' | 'analyzing' | 'result' | 'saving' | 'error'
const analysisResult = ref(null)
const selectedFile = ref(null)
const previewUrl = ref(null)
const errorMessage = ref('')
const sizeError = ref('')

const MAX_SIZE_BYTES = 10 * 1024 * 1024  // 10MB

function onFileSelected(event) {
  const file = event.target.files[0]
  if (!file) return

  sizeError.value = ''
  if (file.size > MAX_SIZE_BYTES) {
    sizeError.value = '사진 크기가 10MB를 초과합니다. 더 작은 사진을 선택해주세요.'
    event.target.value = ''
    return
  }

  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  startAnalysis()
}

async function startAnalysis() {
  phase.value = 'analyzing'
  try {
    const result = await analyzePhoto(selectedFile.value, inferMealType())
    analysisResult.value = {
      detectedItems: result.detectedItems ?? [],
      totalKcal: result.totalKcal ?? 0,
      aiComment: result.aiComment ?? '',
    }
    phase.value = 'result'
  } catch {
    phase.value = 'error'
    errorMessage.value = '서버 연결에 실패했어요. 잠시 후 다시 시도해주세요.'
  }
}

async function saveMeal() {
  phase.value = 'saving'
  try {
    await recordPhotoMeal(inferMealType(), analysisResult.value.detectedItems)
    router.push('/log')
  } catch {
    phase.value = 'result'
    errorMessage.value = '저장에 실패했어요. 다시 시도해주세요.'
  }
}

function reset() {
  phase.value = 'idle'
  analysisResult.value = null
  selectedFile.value = null
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  sizeError.value = ''
  errorMessage.value = ''
}

function inferMealType() {
  const h = new Date().getHours()
  if (h >= 4 && h < 10) return 'BREAKFAST'
  if (h < 15) return 'LUNCH'
  if (h < 20) return 'DINNER'
  return 'SNACK'
}
</script>
```

- [ ] **Step 3: router/index.js — PlaceholderView를 MealPhotoView로 교체**

`frontend/src/router/index.js` 상단 import에 추가:
```js
import MealPhotoView from '@/views/MealPhotoView.vue';
```

라우트 수정:
```js
{
  path: 'meals/photo',
  name: 'meal-photo',
  component: MealPhotoView,  // PlaceholderView → MealPhotoView
  meta: {
    title: '사진으로 기록',
  },
},
```

- [ ] **Step 4: 전체 빌드 확인**

```bash
cd backend
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add frontend/src/api/meal.js
git add frontend/src/views/MealPhotoView.vue
git add frontend/src/router/index.js
git commit -m "feat(meal): MealPhotoView + 라우터 + meal API 함수 구현"
```

---

### Task 7: ADR 기록

**Files:**
- Create: `docs/adr/2026-06-23-vision-ai-photo-meal.md`

- [ ] **Step 1: ADR 작성**

`docs/adr/2026-06-23-vision-ai-photo-meal.md`:

```markdown
# ADR: Vision AI 사진 식단 분석 설계 결정 3건
**Date:** 2026-06-23

---

## ADR-1: DB 음식 검색 대신 Claude Vision 직접 영양소 추정

### Context
사진으로 식단 기록 시 음식 영양소를 얻는 방법 결정 필요.
Option A: Claude Vision으로 음식 이름만 감지 → 식품안전처 DB에서 영양소 검색
Option B: Claude Vision이 음식 이름 + 영양소를 한 번에 추정

### Decision
Option B — Claude Vision 직접 추정

### Rationale
- 영양소 오차의 주요 원인은 계수가 아니라 그램 추정 오차 → 두 방식의 최종 정확도 차이 미미
- 비빔밥·찌개 등 복합 한식은 DB 매칭 자체가 어려움 (재료 분리 검색 필요)
- API 호출 횟수 1회 감소 (Vision 1회 vs Vision + 검색 N회)
- UX 단순화: DB 불일치 시 사용자 수동 확인 단계 불필요

### Alternatives Considered
- **DB 검색:** 정확한 공식 데이터 사용 가능하나 복합 음식 처리 어렵고 UX가 끊김

### Consequences
- AI 추정값이므로 동일 음식도 매번 수치가 달라질 수 있음
- `food_code IS NULL` 필터로 AI 추정 아이템 식별 가능

---

## ADR-2: 기존 MealItem 필드 재사용 (스키마 변경 없음)

### Context
AI 추정 아이템 저장 시 기존 MealItem 스키마를 확장할지 결정 필요.
처음 설계 시 5개 컬럼 추가 예정이었으나 실제 엔티티 분석 후 재검토.

### Decision
기존 `calories`, `protein`, `carbs`, `fat` 필드를 AI 추정값으로 그대로 사용.
`foodCode=null`, `foodName=AI 감지 음식명`으로 저장. 스키마 변경 없음.

### Rationale
- MealItem의 영양소 필드는 이미 double 타입으로 선언됐고 JPA 기본값이 nullable
- 새 컬럼 5개 추가 시 Flyway 마이그레이션 + 기존 합산 로직 분기 처리 필요
- `DailySummaryService`의 영양소 합산 쿼리는 foodCode와 무관하게 MealItem 필드 직접 합산
- `fromAiEstimate()` 팩토리 메서드 하나로 처리 가능 — 복잡도 0

### Alternatives Considered
- **5개 컬럼 추가:** 명시적이지만 DB 마이그레이션 + 합산 로직 분기 비용 발생
- **별도 AiMealItem 엔티티:** 과도한 복잡도 증가, 합산 로직 이원화

### Consequences
- `foodCode IS NULL`로 AI 아이템 식별
- AI 아이템의 `fiber`는 0.0으로 저장 (Claude가 식이섬유를 추정하지 않음)

---

## ADR-3: 이미지를 multipart 대신 base64 JSON으로 전송

### Context
Vue → Spring 이미지 전송 방식 결정.
Option A: `multipart/form-data`로 바이너리 전송
Option B: FileReader API로 base64 변환 후 JSON body로 전송

### Decision
Option B — base64 JSON

### Rationale
- 기존 `apiClient.post()`가 JSON body 전용으로 구현됨 → multipart 지원 추가 시 apiClient 수정 필요
- Vue에서 FormData의 `Content-Type` 경계값 자동 처리 문제 (apiClient가 강제로 json 헤더 설정)
- base64 → Spring → FastAPI 전달 시 형식 일관성 유지 (FastAPI는 JSON 전용)
- 모바일 사진 1~3MB → base64 인코딩 후 ~4MB → 실용적 범위

### Alternatives Considered
- **multipart/form-data:** 네트워크 효율적이지만 apiClient 수정 + Spring MultipartFile 처리 필요
- **별도 fetch 직접 호출:** apiClient 인증 로직 우회 → 토큰 처리 중복

### Consequences
- 10MB 파일은 base64로 ~13.3MB → Vue에서 10MB 클라이언트 사이드 검증으로 차단
- Spring 메모리에 base64 문자열 일시 상주 (최대 ~13MB)
```

- [ ] **Step 2: ADR 커밋**

```bash
git add docs/adr/2026-06-23-vision-ai-photo-meal.md
git commit -m "docs: Vision AI 사진 식단 분석 ADR 3건 기록"
```
