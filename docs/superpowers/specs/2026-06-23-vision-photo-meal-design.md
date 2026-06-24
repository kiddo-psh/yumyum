# Vision AI 사진 식단 분석 설계 스펙

**Date:** 2026-06-23
**Feature:** 사진 촬영 → Claude Vision 음식 감지 → 영양소 추정 → Meal 저장
**Scope:** FastAPI + Spring + Vue 전 레이어

---

## 배경 및 목표

사용자가 식사 사진을 찍으면 Claude Vision API가 음식을 감지하고 영양소를 추정한다.
사용자는 결과를 확인한 뒤 "식단에 추가" 버튼 하나로 Meal 기록을 완성한다.

DB 음식 검색(식품안전처 API)을 거치지 않고 Claude가 영양소를 직접 추정하는 방식을 채택한다.
이유: 그램 추정 오차가 영양소 계수 오차보다 크고, 복합 한식(비빔밥·찌개 등)은 DB 매칭 자체가 어렵다.

---

## 전체 데이터 흐름

```
Vue MealPhotoView
  ① 이미지 선택/촬영
  ② POST /meals/photo/analyze (multipart image) → Spring
       → base64 변환
       → POST /ai/meal/analyze-photo (FastAPI)
           → call_claude_vision() → GMS → Claude Vision
       ← DetectedItem[] + ai_comment
  ③ 결과 표시 (음식 카드 목록 + 총 칼로리 + AI 코멘트)
  ④ 사용자 "식단에 추가" 클릭
  ⑤ POST /meals/photo (confirmed items) → Spring
       → Meal + MealItem 저장 (foodCode=null)
  ⑥ 성공 → /log 이동
```

**FastAPI 미노출 원칙 준수:** Vue는 Spring만 바라보고, Spring이 FastAPI로 내부 프록시.
Spring이 JWT 인증을 먼저 검증한 뒤 FastAPI 호출.

---

## FastAPI

### 신규 엔드포인트

`POST /ai/meal/analyze-photo`

```python
class PhotoAnalyzeRequest(BaseModel):
    image_base64: str        # base64 인코딩 이미지
    media_type: str          # "image/jpeg" | "image/png" | "image/webp"
    meal_type: str           # "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK"

class DetectedItem(BaseModel):
    name: str                # "닭가슴살"
    estimated_grams: float   # 180.0
    kcal: float
    protein_g: float
    carb_g: float
    fat_g: float

class PhotoAnalyzeResponse(BaseModel):
    detected_items: list[DetectedItem]
    total_kcal: float
    ai_comment: str
```

### claude_service.py 확장

기존 `call_claude(prompt)` 텍스트 전용 함수는 변경하지 않는다.
Vision 전용 함수를 별도로 추가한다:

```python
async def call_claude_vision(
    image_base64: str,
    media_type: str,
    prompt: str,
    model: str | None = None,
    max_tokens: int = 800,
) -> str
```

`_call_gms()`를 content 블록 방식으로 확장한다:
```python
"messages": [{
    "role": "user",
    "content": [
        {"type": "image", "source": {"type": "base64", "media_type": media_type, "data": image_base64}},
        {"type": "text", "text": prompt}
    ]
}]
```

vision 모델은 `claude-opus-4-5-20251101` 사용 (CLAUDE.md: RAG/심화 기능에 opus 사용 기준).
dev 환경에서는 mock 응답 반환 (Claude API 호출 없음).

### 프롬프트 전략

```
이 사진에서 음식을 모두 감지하고 JSON으로 반환하세요.
식사 유형: {meal_type}
각 음식마다: name(한국어), estimated_grams, kcal, protein_g, carb_g, fat_g
감지 불가 시 detected_items는 빈 배열로 반환.
반드시 JSON만 반환.
```

### 파일 위치
- `app/routers/ai_meal.py` — 엔드포인트 추가 (기존 파일)
- `app/schemas/meal.py` — PhotoAnalyzeRequest, DetectedItem, PhotoAnalyzeResponse 추가
- `app/services/claude_service.py` — call_claude_vision() 추가
- `app/main.py` — 라우터 등록 변경 없음 (ai_meal 이미 등록됨)

---

## Spring

### 신규 엔드포인트 2개

**`POST /meals/photo/analyze`** — 분석만, DB 저장 없음
```
Request:  multipart/form-data { image: MultipartFile, mealType: String }
Response: PhotoAnalysisResponse { detectedItems: [...], totalKcal, aiComment }
```

**`POST /meals/photo`** — Meal + MealItem 저장
```
Request:  PhotoMealRequest { mealType, items: [{name, estimatedGrams, kcal, protein, carb, fat}] }
Response: MealResponse (기존 형식)
```

### MealItem 스키마 변경

```sql
-- 실제 테이블명은 MealItem 엔티티의 @Table(name=...) 확인 필요 (meal_item 또는 meal_items)
-- food_code: NOT NULL → NULL 허용
-- AI 추정 아이템용 컬럼 추가
ALTER TABLE meal_item
  MODIFY COLUMN food_code VARCHAR(255) NULL,
  ADD COLUMN item_name     VARCHAR(255) NULL,
  ADD COLUMN est_kcal      DOUBLE       NULL,
  ADD COLUMN est_protein   DOUBLE       NULL,
  ADD COLUMN est_carb      DOUBLE       NULL,
  ADD COLUMN est_fat       DOUBLE       NULL;
```

`food_code IS NULL`이면 est_* 컬럼으로 영양소 합산.
`DailySummaryService` 등 기존 영양소 합산 로직에서 분기 처리 필요.

### 파일 위치
- `nutrition/domain/MealItem.java` — nullable 필드 + est_* 필드 추가
- `nutrition/application/MealItemCommand.java` — AI 아이템용 필드 추가
- `nutrition/application/MealService.java` — foodCode null 분기
- `nutrition/infrastructure/client/AiMealClient.java` — analyzePhoto() 추가
- `nutrition/infrastructure/client/dto/` — PhotoAnalyzeClientRequest/Response
- `nutrition/presentation/MealController.java` — 2개 엔드포인트 추가
- `nutrition/presentation/dto/` — PhotoAnalysisResponse, PhotoMealRequest

---

## Vue

### MealPhotoView.vue (신규)

`frontend/src/views/MealPhotoView.vue`로 생성, 라우터에서 `PlaceholderView` 대체.

**상태 머신:**

| phase | 화면 |
|---|---|
| `idle` | 파일 선택 영역 + 카메라 아이콘 |
| `analyzing` | 로딩 스피너 + "AI가 음식을 분석하는 중..." |
| `result` | 감지된 음식 카드 목록 + 총 칼로리 + ai_comment + "식단에 추가" 버튼 |
| `saving` | 버튼 disabled + "..." |
| `success` | 성공 토스트 → `/log`로 라우팅 |
| `error` | 에러 메시지 + 재시도 버튼 |

파일 선택: `<input type="file" accept="image/*" capture="environment">` — 모바일 카메라 직접 열기 지원.

---

## 에러 처리

| 케이스 | 처리 |
|---|---|
| 음식 감지 실패 (빈 detected_items) | "음식을 찾지 못했어요. 다시 촬영해보세요." + 재시도 |
| Claude API 오류 (5xx) | "분석 서버에 문제가 생겼어요." + 재시도 |
| 이미지 너무 큼 (>10MB) | Vue에서 클라이언트 사이드 검증, 업로드 전 차단 |
| 저장 실패 | 분석 결과는 유지, 저장 버튼 재활성화 |

---

## 테스트 전략

**FastAPI (pytest):**
- `test_analyze_photo_mock`: dev 환경에서 mock 응답 반환 확인
- `test_call_claude_vision_payload`: GMS에 전송하는 payload에 image content block 포함 확인
- `test_analyze_photo_empty`: Claude가 빈 detected_items 반환 시 정상 처리

**Spring:**
- `AiMealClientTest`: `analyzePhoto()` WebClient 호출 확인 (MockWebServer)
- `MealServiceTest`: foodCode=null MealItem 저장 및 영양소 합산 확인
- `MealControllerTest`: `/meals/photo/analyze`, `/meals/photo` 엔드포인트 통합 테스트

**Vue:**
- 수동 E2E: 실제 사진으로 분석 → 확인 → `/log`에서 기록 확인

---

## 기록 예정 ADR

- ADR: Claude Vision 직접 추정 vs DB 검색 선택 이유
- ADR: foodCode nullable 방식으로 기존 MealItem 재사용한 이유 (별도 엔티티 미생성)
