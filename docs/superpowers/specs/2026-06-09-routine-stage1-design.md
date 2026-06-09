# 루틴 1단계 설계: 루틴 등록 + AI 루틴 생성

**날짜:** 2026-06-09  
**범위:** F901 루틴 등록, Spring ↔ FastAPI 연동 포인트 #3  
**대상 브랜치:** feature/F901-routine-create

---

## 1. 개요

- 루틴 없는 신규 사용자 → AI(FastAPI)가 split 기반 운동 종목·세트·반복·무게 전체 생성
- 루틴 있는 사용자 → 직접 입력
- 생성 후 종목·무게·세트 자유 수정 가능
- 한 회원이 여러 루틴 소유 가능

---

## 2. 도메인 모델

### Spring Boot 엔티티

**`Routine`**
| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| memberId | Long | 회원 FK |
| name | String | 루틴 이름 |
| daysPerWeek | int | 주 운동 횟수 |
| isAiGenerated | boolean | AI 생성 여부 |
| createdAt | LocalDateTime | 생성일시 |

**`RoutineExercise`**
| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| routineId | Long | Routine FK |
| dayLabel | String | 운동 일자 레이블 (예: "상체", "하체", "가슴/삼두") |
| exerciseName | String | 운동 종목명 (자유 문자열) |
| targetSets | int | 목표 세트 수 |
| targetReps | int | 목표 반복 수 |
| targetWeightKg | double | 목표 무게 (kg) |
| orderIndex | int | 같은 dayLabel 내 순서 |

### SplitType enum (Spring 상수)

```java
public enum SplitType {
    // 주 2회
    FULL_BODY_2,                      // 전신/전신
    // 주 3회
    FULL_BODY_3,                      // 전신/전신/전신
    UPPER_LOWER_FULL,                 // 상체/하체/전신
    // 주 4회
    UPPER_LOWER_4,                    // 상체/하체/상체/하체
    PUSH_PULL_LEGS_UPPER,             // 가슴삼두/등이두/하체/어깨
    // 주 5회
    PUSH_PULL_LEGS_UPPER_LOWER,       // 밀기/당기기/하체/상체/하체
    PPLFU                             // 가슴삼두/등이두/하체/전신/상체
}
```

SplitType마다 `splitLabels` (String 배열) 매핑을 Spring 내부 상수로 관리한다.

---

## 3. API 명세

### Spring Boot

#### `GET /routines/split-options`
주 운동 횟수에 맞는 SplitType 목록 반환. FastAPI 호출 없음.

**요청:**
```
GET /routines/split-options?daysPerWeek=4
```

**응답 `200 OK`:**
```json
[
  { "splitType": "UPPER_LOWER_4",       "label": "상체/하체/상체/하체" },
  { "splitType": "PUSH_PULL_LEGS_UPPER","label": "가슴삼두/등이두/하체/어깨" }
]
```

---

#### `POST /routines/ai`
FastAPI를 호출하여 AI 루틴 생성 후 저장.

**요청:**
```json
{
  "memberId": 1,
  "daysPerWeek": 4,
  "splitType": "UPPER_LOWER_4"
}
```

**응답 `201 Created`:**
```json
{
  "routineId": 1,
  "name": "4일 상체/하체 분할 루틴",
  "daysPerWeek": 4,
  "isAiGenerated": true,
  "exercises": [
    {
      "id": 1,
      "dayLabel": "상체",
      "exerciseName": "벤치프레스",
      "targetSets": 4,
      "targetReps": 8,
      "targetWeightKg": 60.0,
      "orderIndex": 0
    }
  ],
  "aiComment": "..."
}
```

> `memberId`는 JWT 도입 후 SecurityContext에서 추출 예정.

---

#### `POST /routines`
사용자 직접 입력 루틴 등록. FastAPI 호출 없음.

**요청:**
```json
{
  "memberId": 1,
  "name": "내 푸시 루틴",
  "daysPerWeek": 3,
  "exercises": [
    {
      "dayLabel": "가슴/삼두",
      "exerciseName": "벤치프레스",
      "targetSets": 4,
      "targetReps": 8,
      "targetWeightKg": 70.0,
      "orderIndex": 0
    }
  ]
}
```

**응답 `201 Created`:** `POST /routines/ai`와 동일 구조 (`isAiGenerated: false`).

---

#### `PATCH /routines/{routineId}/exercises/{exerciseId}`
운동 종목·무게·세트·반복 수정.

**요청:**
```json
{
  "exerciseName": "인클라인 벤치프레스",
  "targetSets": 3,
  "targetReps": 10,
  "targetWeightKg": 55.0
}
```

**응답 `200 OK`:** 수정된 RoutineExercise 반환.

---

### FastAPI

#### `POST /ai/routine/generate`

**요청:**
```json
{
  "gender": "M",
  "age": 25,
  "weight_kg": 75.0,
  "height_cm": 178.0,
  "health_goal": "MUSCLE_GAIN",
  "days_per_week": 4,
  "split_type": "UPPER_LOWER_4",
  "split_labels": ["상체", "하체", "상체", "하체"]
}
```

**응답:**
```json
{
  "routine_name": "4일 상체/하체 분할 루틴",
  "days": [
    {
      "day_label": "상체",
      "exercises": [
        { "name": "벤치프레스", "sets": 4, "reps": 8, "weight_kg": 60.0 },
        { "name": "덤벨 숄더프레스", "sets": 3, "reps": 10, "weight_kg": 18.0 }
      ]
    },
    {
      "day_label": "하체",
      "exercises": [
        { "name": "바벨 스쿼트", "sets": 4, "reps": 8, "weight_kg": 80.0 }
      ]
    }
  ],
  "ai_comment": "근육량 증가를 위해 복합운동 위주로 구성했습니다. 점진적으로 무게를 늘려보세요!"
}
```

Claude 프롬프트에 신체 정보·목표·split 구조를 포함하여 JSON 형식으로 응답 요청.

---

## 4. 데이터 흐름

### AI 루틴 생성 (`POST /routines/ai`)

```
클라이언트 → POST /routines/ai { memberId, daysPerWeek, splitType }

Spring:
  1. Member 조회 (신체정보)
  2. SplitType → split_labels 변환 (상수 매핑)
  3. POST /ai/routine/generate 호출 (RestClient)

FastAPI:
  4. Claude API로 운동 종목·세트·반복·무게 생성
  5. JSON 응답 반환

Spring:
  6. Routine 저장
  7. RoutineExercise 저장 (days 펼쳐서 flat하게 저장)
  8. 201 Created 응답
```

### 직접 수정 (`PATCH /routines/{id}/exercises/{exerciseId}`)

```
클라이언트 → PATCH { exerciseName, targetSets, targetReps, targetWeightKg }
Spring:
  1. RoutineExercise 조회
  2. 필드 업데이트
  3. 200 OK 응답
```

---

## 5. 예외 처리

| 상황 | HTTP | 메시지 |
|---|---|---|
| Member 없음 | 404 | "회원을 찾을 수 없습니다" |
| Routine 없음 | 404 | "루틴을 찾을 수 없습니다" |
| RoutineExercise 없음 | 404 | "운동을 찾을 수 없습니다" |
| FastAPI 연결 실패 | 503 | "AI 서버에 연결할 수 없습니다" |

기존 `GlobalExceptionHandler`에 추가 처리.

---

## 6. 범위 외 (2단계 이후)

- `RoutineSession`, `SessionSet` 엔티티
- `POST /routines/sessions` 세션 기록
- `POST /ai/routine/adjust` UP/HOLD/DOWN/VOLUME_UP/DELOAD 판단
- `GET /ai/routine/weekly-plan/{id}/{week}` 주차별 조회
