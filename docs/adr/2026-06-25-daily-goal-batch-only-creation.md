# ADR: DailyGoal 생성은 새벽 4시 배치 전용으로 유지, GET 시 lazy 생성 금지
**Date:** 2026-06-25
**작성자:** g0rnn

## Context

온보딩 당일의 `DailyGoal`은 `ProgramService.create()`가 즉시 생성하지만, 그 이후 날짜들의 `DailyGoal`은
새벽 4시 배치(`DailyBatchJob` → `DailyGoalCreationService.createForActivePrograms()`)가 활성 Program 전체를 대상으로 미리 생성해두는 구조였다.

이 작업 도중, 별도로 머지된 PR(#75 "DailyGoal 배치 선행 생성 제거, lazy 생성으로 전환")이 다음과 같이 구조를 바꿔놓았다:
- `DailyGoalCreationService`에 `ensureGoalExists(memberId, date)`를 추가
- `DailySummaryService.getSummary()`, `CalorieBalanceService.getBalance()` 같은 **GET 조회 메서드**의 첫 줄에서 `ensureGoalExists()`를 호출해, DailyGoal이 없으면 그 자리에서 생성
- `DailyBatchJob`에서 `DailyGoalCreationService` 호출을 제거 (주석: "DailyGoal은 API 호출 시 lazy 생성 — 배치 선행 생성 불필요")

## Decision

`ensureGoalExists()`를 제거하고, `DailyBatchJob`이 새벽 4시에 `DailyGoalCreationService.createForActivePrograms()`를 호출하는 기존 구조로 되돌린다.
`DailySummaryService`, `CalorieBalanceService`를 비롯한 모든 GET 핸들러는 `DailyGoal`을 **읽기만** 하고, 없으면 없는 그대로(0 또는 기본값) 응답한다.

## Rationale

GET은 읽기 전용이어야 한다. 조회 요청만으로 DB에 새 행이 생기는 것은 호출자가 예측할 수 없는 부작용이며,
같은 GET을 여러 번 호출했을 때 동작이 달라진다 (첫 호출만 INSERT 발생, 이후엔 단순 SELECT) — 캐싱, 재시도, 모니터링/헬스체크성 호출 등에서 디버깅을 어렵게 만든다.

배치가 새벽 4시에 활성 Program 전체에 대해 그날의 `DailyGoal`을 한 번에 만들어두면, 이후의 모든 조회는 멱등성 있는 단순 SELECT가 된다.

## Alternatives Considered

- **lazy 생성 유지**: 거부. 위 이유와 동일.
- **배치 + lazy fallback 동시 운영** (배치가 실패했을 때만 GET이 보완 생성): 거부. 두 개의 생성 경로가 동시에 존재하면 같은 날짜에 대한 동시 INSERT 시도(동시성 문제) 및 "이번엔 어느 경로로 생성됐는지" 추적의 복잡도가 늘어난다.
- **GET 응답에 캐시를 둬서 첫 호출의 부작용을 숨김**: 거부. 부작용 자체가 문제이지, 부작용이 보이는지 여부가 문제가 아니다.

## Consequences

- 배치가 실패하거나 비정상적으로 늦게 실행되면, 그날치 `DailyGoal`이 생성되지 않은 채로 남는다 — 별도 모니터링/알람이나 수동 재실행(`DailyGoalCreationService.createForActivePrograms()` 수동 호출)이 필요하다.
- 온보딩 당일은 `ProgramService.create()`가 동기적으로 `DailyGoal`을 생성하므로 이 결정의 영향을 받지 않는다.
- 새로운 도메인에서 "조회 시점에 없으면 생성"하는 패턴이 다시 제안된다면, 이 ADR을 근거로 배치/이벤트 기반 생성으로 대체할 것을 우선 검토한다.
