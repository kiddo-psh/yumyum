# ADR: 새벽 04:00 cutoff 규칙을 EffectiveDateResolver / getEffectiveToday 공유 유틸로 추출
**Date:** 2026-06-25
**작성자:** g0rnn

## Context

"새벽 04:00 이전 식사 기록은 전날 `DailyGoal`에 반영된다"는 도메인 규칙이 있다.
`MealService`는 이 규칙을 `effectiveDateOf(LocalDateTime)`라는 private 메서드로 구현해 식사 기록에만 적용하고 있었다.

같은 규칙이 적용돼야 하는 다른 지점들은 각자 따로 날짜를 계산하고 있었다:
- `ProgramOnboardingListener`가 온보딩 완료 시 Program/DailyGoal 시작일을 `LocalDate.now()`(cutoff 미적용)로 계산
- 프론트 `LogView`/`HomeView`/`MealActionView`/`DashboardView`가 "오늘"을 `new Date()`(cutoff 미적용)로 계산

자정~새벽 4시 사이에 온보딩하거나 식단 화면을 열면, 식사는 전날 기준으로 쌓이는데 Program 시작일·프론트 "오늘"은 당일 기준으로 계산되어 서로 어긋나는 버그가 실제로 발생했다 ([[2026-06-25-today-calories-not-updating]] 참고).

## Decision

04:00 cutoff 규칙을 백엔드 `EffectiveDateResolver`(`global/time`), 프론트 `getEffectiveToday()`(`src/utils/effectiveDate.js`) 두 개의 단일 유틸로 추출한다.

규칙이 필요한 모든 지점이 이 유틸을 거치도록 통일한다:
- 백엔드: `MealService.record()` / `recordFromPhoto()`, `ProgramOnboardingListener`, `MealController.list()`의 날짜 기본값
- 프론트: `LogView`(`currentDate`/`isToday`/`displayDate`/`goToday()`), `HomeView`(`today`), `MealActionView`(`todayDate`), `DashboardView`(`today`)

## Rationale

같은 비즈니스 규칙이 여러 곳에 흩어져 있으면, 한 곳만 고쳤을 때 나머지가 조용히 어긋난다 — 실제로 `MealService`만 cutoff를 적용하고 onboarding/프론트는 적용하지 않아 발생한 버그였다.

백엔드와 프론트는 언어가 달라 유틸을 공유할 수 없으므로, 동일한 이름·동일한 동작의 유틸을 양쪽에 각각 두고 "이 규칙이 필요하면 반드시 이 함수를 거친다"는 컨벤션으로 동기화를 강제한다.

## Alternatives Considered

- **각 호출부에 인라인으로 동일 로직 중복**: 거부. 정확히 이번 버그의 원인이었다.
- **DB 트리거/뷰로 effective_date 계산**: 거부. 프론트도 동일한 "오늘" 개념을 알아야 하므로 DB에만 규칙을 두면 화면 표시값이 여전히 어긋난다.
- **백엔드가 응답에 effective date를 내려주고 프론트는 그 값만 그대로 사용**: 차후 검토 가능하지만, 모든 화면 전환(이전/다음 날짜 이동 등)마다 서버 라운드트립이 필요해 현재로선 과한 복잡도로 판단해 보류.

## Consequences

- `EffectiveDateResolver`(Java)와 `getEffectiveToday()`(JS)는 동일 규칙을 언어별로 독립 구현한 것이므로, import로 강제 동기화할 수 없다. cutoff 시간(현재 04:00)이 바뀌면 두 파일을 모두 수정해야 하며, 누락 위험은 코드 리뷰와 이 문서로만 완화한다.
- 향후 날짜 경계가 관련된 신규 기능(예: 주간 리포트 시작일)도 반드시 이 유틸을 거쳐야 한다.
