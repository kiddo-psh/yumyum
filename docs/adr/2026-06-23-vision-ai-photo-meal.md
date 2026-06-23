# ADR: Vision AI 사진 식단 분석 설계 결정 3건

**Date:** 2026-06-23  
**Status:** Accepted

---

## ADR-1: DB 음식 검색 대신 Claude Vision 직접 영양소 추정

### Context
사진으로 식단 기록 시 음식 영양소를 얻는 방법 결정 필요.
- **Option A:** Claude Vision으로 음식 이름만 감지 → 식품안전처 DB에서 영양소 검색
- **Option B:** Claude Vision이 음식 이름 + 영양소를 한 번에 추정

### Decision
**Option B — Claude Vision 직접 추정**

### Rationale
- **영양소 오차의 주요 원인은 계수가 아니라 그램 추정 오차**
  - DB 매칭 방식: Vision으로 감지한 음식명의 정확도 + DB 검색 정확도 + 그램 추정 오차
  - Vision 직접 추정: Vision의 그램 추정 오차만 발생
  - 최종 영양소 오차는 그램 추정 오차가 지배적 → 두 방식의 정확도 차이 미미
- **복합 한식 처리 용이**
  - 비빔밥, 찌개 등은 여러 재료가 혼합되어 있어 단일 음식명으로 DB 매칭 불가능
  - 재료를 분리해서 각각 검색 필요 → 추가 API 호출, 사용자 개입 필요
- **API 호출 횟수 감소**
  - Vision 1회 호출만으로 음식명 + 영양소 획득
  - DB 검색 방식은 Vision + N회 검색 필요
- **UX 단순화**
  - DB 불일치 또는 검색 실패 시 사용자 수동 확인 단계 제거
  - 한 번의 API 응답으로 완결성 있는 결과 제공

### Alternatives Considered
- **DB 검색:** 정확한 공식 데이터 사용 가능하나 복합 음식 처리 어렵고 UX가 끊김, API 호출 증가

### Consequences
- AI 추정값이므로 동일 음식도 사진 각도, 조명, 포션 차이에 따라 추정값이 달라질 수 있음
- `food_code IS NULL` 필터로 AI 추정 아이템 식별 가능
- Claude의 훈련 데이터 범위 내에서만 정확도 보장

---

## ADR-2: 기존 MealItem 필드 재사용 (스키마 변경 없음)

### Context
AI 추정 아이템 저장 시 기존 MealItem 스키마를 확장할지 결정 필요.
초기 설계 시 5개 컬럼 추가(ai_estimated_flag, source_type 등)를 예정했으나 실제 엔티티 분석 후 재검토.

### Decision
기존 `calories`, `protein`, `carbs`, `fat` 필드를 AI 추정값으로 그대로 사용.
- `foodCode = null` (AI 추정 아이템 마커)
- `foodName = AI 감지 음식명`
- **스키마 변경 없음**

### Rationale
- **기존 필드 충분함**
  - MealItem의 영양소 필드(`calories`, `protein`, `carbs`, `fat`)는 이미 double 타입
  - JPA 기본값이 nullable이므로 foodCode null 상태에서도 영양소 저장 가능
- **마이그레이션 비용 제거**
  - 새 컬럼 5개 추가 시 Flyway 마이그레이션 필요
  - 기존 데이터 스키마 확장 비용 및 위험성 증가
- **합산 로직 분기 불필요**
  - `DailySummaryService`의 영양소 합산 쿼리는 foodCode와 무관하게 MealItem 필드 직접 합산
  - `SUM(protein), SUM(carbs), SUM(fat)` 같은 단순 집계만으로 처리
  - DB-level에서 AI 추정값과 DB 조회값을 동일하게 취급 가능
- **명시성 확보**
  - `MealItem.fromAiEstimate()` 팩토리 메서드 하나로 처리
  - 호출 처에서 AI 추정 아이템임이 명확히 드러남
  - 복잡도 제로

### Alternatives Considered
- **5개 컬럼 추가:** 명시적이지만 DB 마이그레이션 + 합산 로직 분기 처리 비용 발생
- **별도 AiMealItem 엔티티:** 과도한 복잡도 증가, 합산 로직 이원화, 쿼리 복잡도 증가

### Consequences
- `foodCode IS NULL` 쿼리로 AI 추정 아이템만 식별 필요
- AI 아이템의 `fiber`는 0.0으로 저장 (Claude Vision이 식이섬유를 추정하지 않음)
- 향후 AI 아이템 통계/분석이 필요하면 `foodCode` 필터링으로 구분

---

## ADR-3: 이미지를 multipart 대신 base64 JSON으로 전송

### Context
Vue → Spring 이미지 전송 방식 결정.
- **Option A:** `multipart/form-data`로 바이너리 전송 (네트워크 효율적)
- **Option B:** FileReader API로 base64 변환 후 JSON body로 전송 (형식 일관성)

### Decision
**Option B — base64 JSON**

### Rationale
- **기존 apiClient 호환성**
  - 현재 `apiClient.post()`는 JSON body 전용으로 구현됨
  - multipart 지원 추가 시 별도 메서드 작성 또는 전체 apiClient 수정 필요
  - Vue 프로젝트에서 이미 모든 API 호출이 JSON 기반 → 추가 API 메서드는 인증 로직 중복
- **Content-Type 충돌 회피**
  - apiClient가 자동으로 `Content-Type: application/json` 설정
  - multipart 사용 시 FormData 객체의 boundary 자동 생성을 apiClient가 방해할 수 있음
  - base64는 Content-Type 걱정 없이 일반 JSON body로 전송
- **FastAPI 호환성**
  - FastAPI도 JSON 전용 처리
  - Spring → FastAPI 전달 시 base64 문자열을 그대로 전달 가능
  - 형식 변환 없이 end-to-end 일관성 유지
- **모바일 사진 범위 내 실용성**
  - 일반적인 모바일 사진 1~3MB → base64 인코딩 후 ~4MB
  - Vue에서 클라이언트 사이드 검증으로 10MB 초과 차단 가능
  - 실용적 범위 내에서 네트워크 오버헤드 허용 가능

### Alternatives Considered
- **multipart/form-data:**
  - 장점: 네트워크 효율적 (base64 대비 ~25% 더 작음)
  - 단점: apiClient 수정 필요, Spring에서 MultipartFile 처리 및 base64 디코딩 추가 코드 필요
- **별도 fetch 직접 호출:**
  - 장점: multipart 네이티브 지원
  - 단점: apiClient의 인증 로직(JWT 헤더 자동 추가) 우회 → 토큰 처리 중복 발생

### Consequences
- 10MB 파일은 base64로 ~13.3MB → Vue에서 클라이언트 사이드 검증으로 10MB 상한 설정
- Spring 메모리에 base64 문자열 일시 상주 (최대 ~13MB)
  - base64 디코딩 후 즉시 FastAPI로 전달하여 메모리 해제
- 초저속 네트워크에서는 multipart 대비 ~33% 더 느린 전송 시간 (mobile 4G 환경에서 체감 차이 미미)
