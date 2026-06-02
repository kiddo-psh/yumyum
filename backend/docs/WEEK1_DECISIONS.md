# 1주차 구현 결정 기록

> 로그인/회원가입, Docker 환경 설정을 제외한 1주차 기능 구현을 위한 설계 결정 사항.

---

## Q1. DailyGoal 퍼시스턴스 방식

**결정: DB에 날짜별 레코드로 저장**

- `(memberId, date, targetValue, achievedValue, achieved)` 형태로 `daily_goals` 테이블에 저장
- Streak 판정 시 달성 이력을 직접 조회 가능하고, 히스토리도 보존됨

---

## Q2. Streak 업데이트 시점

**결정: Meal 기록 시마다 즉시 재계산**

- `MealItem` 저장 → 오늘의 총 칼로리 합산 → `DailyGoal.recalculate()` 호출
- `achieved`가 `false → true`로 바뀌는 순간 `StreakService.increment()` 호출
- 별도 배치 불필요, 달성 즉시 실시간 피드백 가능

---

## Q3. Food 데이터 초기화 방법

**결정: SQL seed로 더미 데이터 30~50개**

- `data.sql` 또는 Flyway migration으로 자주 쓰는 음식 30~50개 사전 삽입
- MFDS 연동은 2차 MVP 이후로 미룸
- 1주차 목표는 Meal → DailyGoal → Streak 흐름 end-to-end 완성

---

## Q4. DailyGoal 레코드 생성 시점

**결정: 새벽 4시 배치로 당일 DailyGoal 선생성**

- 매일 새벽 4시에 전체 ACTIVE Member의 당일 DailyGoal을 미리 생성
- targetValue는 활성 Program의 `targetCalories` 사용, Program 없으면 성별 기본값 (남성 2400 / 여성 1800 kcal)

### Q4-1. 새벽 4시 이전 Meal 기록 처리

**결정: 자정~03:59 Meal은 전날 DailyGoal에 적용**

- 자정~03:59에 기록된 Meal은 전날(어제) DailyGoal에 포함
- 전날 DailyGoal이 존재하지 않으면 Meal만 저장하고 DailyGoal/Streak 연동 없음

---

## Q5. MealItem 칼로리 계산 및 저장 방식

**결정: 서버에서 계산 후 MealItem에 비정규화 저장**

- 계산식: `Food.caloriesPer100g * amountGrams / 100`
- MealItem에 `calories` 컬럼으로 저장
- DailyGoal 합산 시 `SUM(calories)` 단일 쿼리로 처리
- Food 영양 정보가 변경되어도 기록 당시 값이 보존됨

---

## Q6. Program의 1차 MVP 범위

**결정: AI 없이 수동 생성**

- Member가 직접 `startDate`, `endDate`, `type(DIET/MUSCLE/HEALTH/DISEASE)` 입력
- TDEE는 서버에서 공식으로 계산
- AI 연동 준비 완료 후 생성 로직만 교체

---

## Q7. TDEE 계산에 필요한 Member 필드

**결정: 온보딩에서 5개 필드 필수 수집**

| 필드 | 설명 |
|---|---|
| `sex` | 성별 (남/여) |
| `birthYear` | 출생연도 |
| `heightCm` | 키 (cm) |
| `weightKg` | 체중 (kg) |
| `activityLevel` | 활동량 (SEDENTARY / LIGHTLY_ACTIVE / MODERATELY_ACTIVE / VERY_ACTIVE / EXTRA_ACTIVE) |

**Program type별 TDEE 조정값:**

| type | 조정 |
|---|---|
| `DIET` | TDEE − 500 kcal |
| `MUSCLE` | TDEE + 300 kcal |
| `HEALTH` | TDEE (유지) |
| `DISEASE` | TDEE (유지) |

TDEE 계산 공식: **Mifflin-St Jeor**

---

## Q8. TDEE 계산값 저장 위치

**결정: Program 생성 시 계산해서 `Program.targetCalories`로 저장**

- 배치는 `Program.targetCalories` 값을 그대로 DailyGoal에 적용
- Program 기간 중 targetCalories는 고정 (체중 변화 반영 안 함)
- 체중이 바뀌면 Program을 새로 생성하는 것이 도메인 의도에 부합

---

## Q9. Food 검색 API 방식

**결정: 이름 키워드 검색 단일 API**

```
GET /foods?query=닭가슴살
```

- `LIKE %keyword%` 쿼리로 구현
- MFDS 연동으로 데이터가 늘어나도 인터페이스 변경 없이 재사용 가능

---

## Q10. MealItem 영양소 비정규화 범위

**결정: 5가지 영양소 모두 MealItem에 비정규화 저장**

저장 시점에 Food를 조회해서 아래 값을 모두 계산해 저장:

- `calories` (kcal)
- `carbs` (g)
- `protein` (g)
- `fat` (g)
- `fiber` (g)

---

## Q11. WeeklyReport의 1차 MVP 처리

**결정: 엔티티/테이블만 정의, 생성 로직은 stub**

- Program이 7일 경과하면 빈 WeeklyReport 레코드 생성 (`content = null`)
- AI 연동 시 `content`만 채우면 됨, 인터페이스 변경 없음

---

## Q12. 개발 중 API 인증 처리

**결정: seed Member + memberId 헤더로 임시 처리**

- seed 데이터로 테스트용 Member 1명 고정 생성 (`memberId = 1`, 5개 신체 정보 포함)
- API 구현 시 memberId는 헤더(`X-Member-Id`)로 받도록 인터페이스 설계
- 로그인 완성 후 SecurityContext에서 추출하도록 교체 (다른 코드 수정 없음)

---

## Q13. Streak 리셋 시점

**결정: 새벽 4시 배치에서 전날 미달성 체크 후 리셋**

- 배치 실행 시 전날 DailyGoal이 `achieved = false`이면 해당 Member의 `currentStreak = 0`으로 리셋
- Q4의 DailyGoal 생성 배치와 동일한 Job에서 처리

---

## Q14. Program 동시 활성 여부

**결정: 동시에 활성 Program 1개만 허용**

- Program 생성 시 이미 활성 Program이 있으면 `400 Bad Request`
- DailyGoal targetValue 계산 시 "활성 Program" 조회가 단순해짐
- CONTEXT.md 도메인 정의("Program이 종료되면 새로운 Program을 시작할 수 있다")와 일치

---

## Q15. Program 종료 처리 방식

**결정: 새벽 4시 배치에서 자동 종료**

- `endDate < 오늘`인 Program을 `COMPLETED` 상태로 전환
- 이후 DailyGoal 생성 시 활성 Program 없음으로 처리 → 성별 기본값 적용

---

## Q16. Meal 조회 API 범위

**결정: 날짜 기반 단일 API**

```
GET /meals?date=2026-06-01
```

- `date` 파라미터 기본값: 오늘
- 특정 날의 전체 Meal 목록과 각 MealItem 반환
- 달력 화면 등 확장 시에도 동일 API 재사용 가능

---

## Q17. 일일 요약 조회 API

**결정: 하루 요약 단일 API**

```
GET /daily-summary?date=2026-06-01
```

응답에 포함되는 데이터:

| 항목 | 출처 |
|---|---|
| `targetCalories` | DailyGoal |
| `achievedCalories` | DailyGoal |
| `achieved` | DailyGoal |
| `currentStreak` | MemberStats |
| `maxStreak` | MemberStats |
| `totalCarbs` | MealItem 합산 |
| `totalProtein` | MealItem 합산 |
| `totalFat` | MealItem 합산 |

---

## 새벽 4시 배치 실행 순서

```
1. endDate < 오늘인 Program → COMPLETED 전환
2. 활성 Program.targetCalories 기준으로 오늘 DailyGoal 생성
   (활성 Program 없으면 성별 기본값 적용)
3. 전날 DailyGoal achieved=false이면 currentStreak = 0 리셋
4. Program 시작일 기준 7일 경과 시 WeeklyReport stub 생성 (content=null)
```

---

## 코드 버그 (구현 전 수정 필요)

**`MemberStats` 엔티티에 `memberId` 필드 누락**

- `StreakService.findByMemberId(memberId)`를 호출하지만 `MemberStats`에 `memberId` 컬럼이 없어 현재 작동 불가
- `@Column private Long memberId` 추가 필요
