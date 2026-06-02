# PRD: 1주차 핵심 기능 — Meal 기록 / DailyGoal / Streak / Program

> 로그인/회원가입, Docker 환경 설정은 별도 작업으로 분리되어 있으며 이 PRD 범위 밖이다.

---

## Problem Statement

Member는 매일의 식단을 기록하고 칼로리 목표 달성 여부를 확인하고 싶다. 그러나 현재 앱에는 식단 기록 수단도, 오늘의 목표도, 연속 달성 동기 부여 지표도 없다. Member는 "내가 오늘 얼마나 먹었는지", "목표를 달성했는지", "얼마나 연속으로 달성했는지"를 한눈에 볼 수 없다.

---

## Solution

Member가 음식(Food)을 검색해서 끼니(Meal)에 식단 항목(MealItem)을 기록하면, 서버가 실시간으로 일일 칼로리 합산 → DailyGoal 달성 여부 갱신 → Streak 증가를 처리한다. Program을 생성하면 TDEE 기반 맞춤 목표가 DailyGoal에 자동 반영되고, 새벽 4시 배치가 하루 단위 상태 전이(DailyGoal 선생성 / Streak 리셋 / Program 종료 / WeeklyReport stub 생성)를 일괄 처리한다.

---

## User Stories

1. As a Member, I want to search for Food by name, so that I can quickly find what I ate without scrolling through a full list.
2. As a Member, I want to record a Meal by selecting a meal type (아침/점심/저녁/간식) and a date, so that I can organize my food intake by meal occasion.
3. As a Member, I want to add multiple MealItems to a Meal, so that I can record a full meal with side dishes.
4. As a Member, I want the server to calculate calories and macros from the amount I enter, so that I don't have to do the math myself.
5. As a Member, I want to see today's Meal list with per-item nutrition info, so that I can review what I've eaten.
6. As a Member, I want to see a daily summary (target calories, achieved calories, macros, Streak), so that I can check my progress at a glance on the home screen.
7. As a Member, I want my DailyGoal to be prepared automatically each morning, so that I don't have to manually set a target every day.
8. As a Member, I want my Streak to increase immediately after I hit my calorie goal, so that I get instant positive feedback.
9. As a Member, I want my Streak to reset to 0 if I miss a day, so that the streak accurately reflects my consistency.
10. As a Member, I want to look up my Meal history by date, so that I can review past eating patterns.
11. As a Member, I want to create a Program with a type and duration, so that I can follow a structured health management plan.
12. As a Member, I want the system to calculate a TDEE-based calorie target when I create a Program, so that my DailyGoal reflects my actual health goal.
13. As a Member, I want only one Program to be active at a time, so that there is no ambiguity about which target applies today.
14. As a Member, I want a Program to automatically complete when its end date passes, so that I don't have to manually close it.
15. As a Member, I want a WeeklyReport record to be created every 7 days of a Program, so that AI analysis can be filled in later without breaking the structure.
16. As a Member, I want late-night meals (자정~03:59) to count toward the previous day's DailyGoal, so that my Streak isn't penalized for eating after midnight.
17. As a Member, I want my calorie target to be based on my sex when I have no active Program, so that I always have a sensible default goal.

---

## Implementation Decisions

### 도메인 패키지 구성 (변경/신규)

- `nutrition`: `Meal`, `MealItem`, `Food` 엔티티, `MealService`
- `program`: `Program`, `DailyGoal`, `WeeklyReport` 엔티티, `ProgramService`, `DailyGoalService`
- `growth`: `MemberStats` 엔티티 (기존), `StreakService` (기존 — `memberId` 필드 추가 버그 수정 포함)
- `member`: `Member` 엔티티 (온보딩 5개 필드 포함), seed 데이터 1건
- `global`: 새벽 4시 배치 `DailyBatchJob`

### 코드 버그 수정

- `MemberStats` 엔티티에 `memberId` 필드 누락. `findByMemberId` 쿼리가 현재 동작하지 않으므로 구현 시작 전 `memberId` 컬럼 추가가 필수.

### DailyGoal 엔티티

- DB에 날짜별 레코드로 저장: `(memberId, date, targetValue, achievedValue, achieved)`
- 새벽 4시 배치에서 당일 레코드를 미리 생성
- `targetValue`: 활성 Program이 있으면 `Program.targetCalories`, 없으면 성별 기본값 (남성 2400 / 여성 1800 kcal)

### MealItem 비정규화

- MealItem 저장 시 `calories`, `carbs`, `protein`, `fat`, `fiber` 5개 영양소를 `Food` 값 기준으로 계산해 컬럼으로 저장
- 계산식: `Food.{nutrient}Per100g × amountGrams / 100`
- DailyGoal 합산은 `SUM(calories)` 단일 쿼리

### Streak 즉시 갱신

- MealItem 저장 → 오늘 날짜의 총 칼로리 합산 → `DailyGoal.recalculate(totalCalories)` → `achieved`가 `false → true`로 전환되는 순간 `StreakService.increment()` 호출
- 동일 날 중복 달성 처리 불필요 (`achieved` 플래그가 한번 `true`가 되면 유지)

### 날짜 경계 규칙

- 자정~03:59에 기록된 Meal은 **전날** DailyGoal에 적용
- 전날 DailyGoal이 없으면 Meal만 저장, DailyGoal/Streak 연동 없음

### TDEE 및 Program

- TDEE 계산 공식: Mifflin-St Jeor
- 입력값: `sex`, `birthYear`, `heightCm`, `weightKg`, `activityLevel` (5단계)
- Program 생성 시 TDEE를 계산해 `Program.targetCalories`로 저장 (Program 기간 중 고정)
- Program type별 조정: `DIET` −500 / `MUSCLE` +300 / `HEALTH`, `DISEASE` 유지
- 동시에 활성 Program은 1개만 허용; 활성 중 생성 시도 시 `400 Bad Request`

### 새벽 4시 배치 (`DailyBatchJob`) — 실행 순서

1. `endDate < 오늘`인 Program → `COMPLETED` 상태 전환
2. 전날 DailyGoal이 `achieved = false`인 Member → `currentStreak = 0` 리셋
3. 활성 Program의 `targetCalories` 기준으로 오늘 DailyGoal 생성 (없으면 성별 기본값)
4. Program 시작일 기준 7일 경과 시 WeeklyReport stub 생성 (`content = null`)

### WeeklyReport

- Program에 종속, 7일 경과 시 빈 레코드(`content = null`) 생성
- AI 연동 시 `content`만 채우면 되므로 인터페이스 변경 없음

### API 목록

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/foods?query={keyword}` | Food 이름 키워드 검색 |
| `POST` | `/meals` | Meal + MealItem 기록 |
| `GET` | `/meals?date={date}` | 날짜별 Meal 조회 (기본값: 오늘) |
| `GET` | `/daily-summary?date={date}` | 일일 요약 (칼로리·영양소·Streak) |
| `POST` | `/programs` | Program 수동 생성 |

### 개발 중 인증

- seed Member 1건 (`memberId = 1`, 5개 신체 정보 포함)
- API 요청 시 `X-Member-Id` 헤더로 memberId 전달
- 로그인 완성 후 SecurityContext 추출로 교체 (API 코드 수정 없음)

### seed 데이터

- Food 30~50개 (`source = MFDS`)를 SQL 또는 Flyway migration으로 사전 삽입
- Member 1건 (테스트용)

---

## Testing Decisions

**좋은 테스트 기준**: 구현 세부사항(private 메서드, 내부 상태)이 아닌 외부 행동(반환값, 상태 변화, 저장 여부)만 검증한다.

### 도메인 단위 테스트 (기존 패턴 — 순수 Java, Spring 없음)

기존 `DailyGoalTest`, `MemberStatsTest` 패턴을 따른다.

- `DailyGoal`: 목표 미달성 / 달성 / 달성 후 값 감소 시 상태 유지 (기존 테스트 존재)
- `MemberStats`: Streak 증가 / 불연속 시 1로 리셋 / maxStreak 갱신 (기존 테스트 존재)
- TDEE 계산 로직: 성별·활동량 조합별 기대값 검증
- `DailyGoal` targetValue 결정 로직: Program 유무 / type별 조정값

### Application Service 테스트 (기존 패턴 — Mockito)

기존 `StreakServiceTest` (Mockito) 패턴을 따른다.

- `MealService.record()`: MealItem 저장 → 칼로리 합산 → DailyGoal 갱신 → Streak 증가 트리거
- `DailyBatchJob.run()`: Program 종료 / DailyGoal 생성 / Streak 리셋 / WeeklyReport stub 생성 각 케이스

### End-to-end 흐름 테스트 (신규 seam)

Meal 기록 → DailyGoal `achievedCalories` 누적 → 목표 초과 시 `achieved = true` + `currentStreak` 증가까지를 하나의 시나리오로 검증한다. Repository는 in-memory H2 또는 실제 DB 사용.

---

## Out of Scope

- 로그인 / 회원가입 / JWT 인증 (별도 작업)
- Docker / 인프라 환경 설정 (별도 작업)
- MFDS 공공데이터 API 연동 (Food seed 데이터로 대체)
- AI 연동 (FastAPI 클라이언트, WeeklyReport 분석 텍스트)
- Nyam 캐릭터, Badge, Level (3차 MVP)
- 운동 기록 (exercise 도메인)
- 커뮤니티, 팔로우 (community 도메인)
- 푸시 알림 (notification 도메인)
- 바코드 스캔

---

## Further Notes

- `DailyGoal`은 현재 코드에서 `@Entity`가 없는 순수 도메인 객체다. 이번 구현에서 `@Entity`로 전환하고 `program` 패키지의 persistence 레이어를 신설해야 한다.
- 새벽 4시 배치는 `@Scheduled(cron = "0 0 4 * * *")`로 구현하되, 로직은 `DailyBatchJob` 단일 클래스가 순서를 조율하고 각 도메인 서비스에 위임하는 구조가 적합하다.
- 온보딩 완료 여부 플래그(`onboardingCompleted`)는 이번 구현에서 별도 관리하지 않는다. seed Member는 처음부터 완료 상태로 간주한다.
