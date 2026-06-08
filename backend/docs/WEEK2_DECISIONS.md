# 2주차 Spring Boot 설계 결정사항

> 작성일: 2026-06-08 (그릴링 세션)
> 범위: F901·F903·F802·G501·G502·Nyam (G101·G102·G104)

---

## Q1. 운동 루틴 도메인 패키지명
**결정: `exercise`**
- 향후 F702 AI 운동 코칭과 자연스럽게 연결됨

---

## Q2. 엔티티 구조
**결정: 3-레벨 구조**
```
Routine          (id, memberId, source, status, startDate)
  └─ RoutineExercise  (id, routineId, weekNumber, 운동명, 계획세트·무게·반복)

RoutineSession   (id, routineId, sessionDate, status)
  └─ SessionSet  (id, sessionId, 세트번호, 실제무게·반복, success)

RoutineSessionFeedback  (id, sessionId: Long, adjustmentResult: JSON)
```

---

## Q3. 루틴 출처 구분
**결정: `source: USER_INPUT | AI_GENERATED`**
- `Food.source`(MFDS/MANUAL/AI)와 동일한 패턴

---

## Q4. RoutineSession 완료 시점
**결정: 명시적 완료 API**
- `PATCH /routines/sessions/{id}/complete`
- 부상·조기 종료 등 부분 완료 케이스 자연스럽게 처리 가능

---

## Q5. FastAPI `/ai/routine/adjust` 호출 방식
**결정: 비동기 (`@Async`)**
- 사용자는 완료 버튼 누르면 다른 페이지로 이동함
- 조정 결과는 `RoutineSessionFeedback`에 나중에 저장

---

## Q6. G501 주간 달성 캘린더 기준
**결정: 이번 주 월~일 기준**
- 달성률 포함 응답 (초록: 100%, 노랑: 50%+, 흰색: 미달성)
- 잔디밭 스타일 캘린더 시각화
- `DailyGoalRepository`에 날짜 범위 조회 메서드 추가 필요

---

## Q7. G502 목표 달성 진행바
**결정: `GET /growth/progress` 별도 엔드포인트**
- 기존 `/daily-summary`와 분리
- 다른 통계 정보 추가 가능성 고려

---

## Q8. Nyam HealthGoal 출처
**결정: `Member`에 `healthGoal` 필드 추가**
```java
@Enumerated(EnumType.STRING)
private HealthGoal healthGoal; // DIET | MUSCLE | HEALTH | DISEASE
```
- `HealthGoal` enum을 `member` 패키지에 신규 정의
- Program과 독립적으로 Nyam 외형에 영향

---

## Q9. Nyam 저장 방식
**결정: Stateless 계산 값 (DB 저장 없음)**
```json
GET /growth/nyam
{
  "appearanceStage": "STAGE_3",
  "healthGoal": "MUSCLE",
  "message": "오늘도 단백질 채우러 가볼까요?",
  "messageType": "ENCOURAGEMENT"
}
```
- `DailyGoal` 달성률 + `Member.healthGoal` 조합으로 매 요청 시 계산

---

## Q10. F802 마지막 끼니 추천 API 구조
**결정: 두 단계 API**
- `GET /meals/last-recommend/available` → `{ "available": true }`
- `GET /meals/last-recommend` → FastAPI 호출 → 추천 3개 반환
- 홈 화면 로드마다 FastAPI 호출 방지
- 트리거 조건: ① 잔여칼로리 1끼 분량 ±20%, ② 오후 5시 이후, ③ 당일 최소 1끼 기록

---

## Q11. FastAPI HTTP 클라이언트
**결정: `RestClient` (Spring 6.1+)**
- `RestTemplate` 후속, 비동기는 `@Async` + `RestClient` 조합
- WebFlux 의존성 불필요

---

## Q12. RoutineSession 시작 방식
**결정: 명시적 시작 API**
- `POST /routines/sessions` → `{ "routineId": 1, "sessionDate": "2026-06-08" }`
- 운동은 식단과 다르게 정해진 시간에 기록됨, 명시적 시작·끝 필요

---

## Q13. AI 조정 결과 저장
**결정: `RoutineSessionFeedback` 별도 테이블 + JSON 컬럼**
```java
@Entity
public class RoutineSessionFeedback {
    @Id @GeneratedValue
    private Long id;
    private Long sessionId;  // plain FK, @OneToOne 없음
    @JdbcTypeCode(SqlTypes.JSON)
    private AdjustmentResult adjustmentResult;
}
```
- MySQL `JSON` 컬럼 타입으로 저장
- `AdjustmentResult`는 Java record로 타입 안전하게 매핑

---

## Q14. Nyam G104 반응 메시지 상황 enum
**결정: 4가지 상황**

| enum | 트리거 |
|---|---|
| `GOAL_ACHIEVED` | 오늘 DailyGoal 달성 |
| `STREAK_MILESTONE` | Streak 3·7·30일 도달 |
| `LAST_MEAL_REMIND` | 마지막 끼니 추천 조건 충족 |
| `DEFAULT` | 그 외 기본 메시지 |

---

## Q15. GET /routines/current "현재 루틴" 정의
**결정: `Routine.status: ACTIVE | INACTIVE`**
- 회원당 ACTIVE 루틴 1개만 허용
- 새 루틴 등록 시 기존 ACTIVE → INACTIVE 자동 전환

---

## Q16. AI 조정 후 RoutineExercise 갱신 방식
**결정: 주차별 계획 버전 관리**
- `RoutineExercise`에 `weekNumber` 컬럼 추가
- AI 조정 결과 수신 시 다음 주차 `RoutineExercise` 레코드 신규 생성
- 4주차 `GET /ai/routine/weekly-plan/{id}/{week}` 연동에 자연스럽게 맞음

```
week 1: 벤치프레스 3×60kg   ← 초기 입력 or AI 생성
week 2: 벤치프레스 3×62.5kg ← AI 조정으로 생성된 레코드
```

---

## Q17. 현재 주차 계산 방식
**결정: `Routine.startDate` 기준 경과 주차**
```java
int weekNumber = (int) ChronoUnit.WEEKS.between(routine.getStartDate(), LocalDate.now()) + 1;
```
- 해당 주차 `RoutineExercise`가 없으면 직전 주차 폴백

---

## Q18. F901 AI_GENERATED 루틴 생성 FastAPI 호출
**결정: 비동기**
- `POST /routines` → `Routine` 저장(ACTIVE, exercises 없음) → 201 반환
- `@Async`: FastAPI `/ai/routine/generate` 호출 → `RoutineExercise(week 1)` 저장
- `GET /routines/current` exercises 빈 배열이면 프론트에서 "생성 중" 표시

---

## 2주차 최종 API 목록

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/routines` | 루틴 등록 (USER_INPUT or AI_GENERATED) |
| `GET` | `/routines/current` | 현재 ACTIVE 루틴 + 이번 주차 exercises |
| `POST` | `/routines/sessions` | 세션 시작 |
| `POST` | `/routines/sessions/{id}/sets` | 세트 기록 |
| `PATCH` | `/routines/sessions/{id}/complete` | 세션 완료 → @Async FastAPI 호출 |
| `GET` | `/growth/weekly-calendar` | 이번 주 월~일 DailyGoal 달성률 |
| `GET` | `/growth/progress` | 오늘 목표 달성 진행바 |
| `GET` | `/growth/nyam` | Nyam 외형 + 반응 메시지 |
| `GET` | `/meals/last-recommend/available` | 끼니 추천 트리거 조건 체크 |
| `GET` | `/meals/last-recommend` | FastAPI 호출 → 끼니 추천 3개 반환 |
