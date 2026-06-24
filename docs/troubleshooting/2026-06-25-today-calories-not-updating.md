# 식단 기록 탭의 "오늘의 칼로리"/"남은 칼로리"가 갱신되지 않음
**Date:** 2026-06-25
**Time spent:** ~2시간 (두 차례에 걸쳐 발견)
**작성자:** g0rnn

## Symptom

식단 기록(`POST /meals`)은 정상적으로 저장되는데, 식단 기록 탭(`LogView`)의 "오늘의 칼로리"/"남은 칼로리"가 0이거나
끼니를 추가해도 변하지 않는다. DB를 초기화하고 서버를 재시작해도 동일하게 재현됨.

## Wrong tracks

- 처음엔 프론트의 `targetCalories`/`intakeCalories`/`calorieProgress` computed 로직 자체를 의심했다.
  실제로는 계산 로직은 정상이었고, API가 빈 응답(`DailyGoal` 없음)을 내려주고 있었다.
- DB를 지우고 재현했을 때, "1차 수정에서 놓친 게 또 있나" 하고 같은 원인을 다시 의심했다.
  실제로는 별개의 원인(팀원이 머지한 PR이 DailyGoal 생성 방식을 바꿔놓음)이 1차 수정과 겹쳐 있었다 — `git pull`을 하지 않았는데도 파일을 다시 읽었을 때 이전에 없던 `ensureGoalExists()`가 보여 혼란을 줬다.

## Root cause

두 가지 원인이 순차적으로 발견됐다.

**1) Effective date 계산이 호출부마다 따로 구현되어 있었음**
`MealService`는 식사 기록 시 "새벽 04:00 이전은 전날"이라는 cutoff를 적용해 `effectiveDate`를 계산했지만,
`ProgramOnboardingListener`는 Program/DailyGoal 시작일을 cutoff 없는 `LocalDate.now()`로 계산했다.
자정~새벽 4시 사이에 온보딩한 사용자는 Program 시작일이 식사 effectiveDate보다 하루 늦게 찍혀, `DailyGoal`을 못 찾는 경우가 생겼다.

**2) GET 조회 시점에 DailyGoal을 lazy 생성하는 방식으로 구조가 바뀌어 있었음**
1)을 고치는 동안, 팀원의 별도 PR이 머지되어 있었다:
- `DailySummaryService`/`CalorieBalanceService`의 GET 메서드가 `DailyGoalCreationService.ensureGoalExists()`를 호출해 그 자리에서 DailyGoal을 생성
- 동시에 `DailyBatchJob`에서 새벽 4시 DailyGoal 생성 호출이 제거됨

이 조합 자체는 동작은 했지만, "조회만 했는데 DB에 행이 생기는" 구조가 됐다 — 자세한 결정 배경은 [[2026-06-25-daily-goal-batch-only-creation]] 참고.

## Fix

- `EffectiveDateResolver`(백엔드) / `getEffectiveToday()`(프론트) 공유 유틸 도입 → [[2026-06-25-effective-date-shared-utility]]
- `ProgramOnboardingListener`가 Program 시작일을 `EffectiveDateResolver.today()`로 계산하도록 변경
- `ensureGoalExists()` 제거, `DailyBatchJob`의 새벽 4시 `DailyGoalCreationService.createForActivePrograms()` 호출 복원
- 프론트 `LogView`/`HomeView`/`MealActionView`/`DashboardView`의 "오늘" 계산을 모두 `getEffectiveToday()`로 통일

## Prevention

- 날짜 경계가 걸린 비즈니스 규칙(이번엔 "새벽 04:00 cutoff")은 한 곳에만 구현하고, 모든 호출부가 그 유틸을 거치도록 강제한다.
- GET 핸들러는 절대 쓰기 작업(엔티티 생성 등)을 하지 않는다 — 코드 리뷰 체크리스트에 추가.
- 팀원과 동시에 같은 영역(이번엔 DailyGoal 생성 시점)을 건드릴 가능성이 있으면, 작업 시작 전에 최신 변경 사항을 먼저 확인한다.
