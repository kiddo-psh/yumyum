# growth 도메인은 도메인 이벤트에 반응한다 (직접 호출 아님)

Streak·Badge 등 growth 도메인의 성장 보상은 발생원 도메인(nutrition·exercise)이 직접 호출하지 않고, 발생원이 발행하는 **사실(fact) 이벤트**를 growth의 `@EventListener`가 수신해 처리한다. 발생원 도메인은 "무엇이 일어났는가"(목표 달성, 운동 기록)만 발행하고, "스트릭을 올려라 / 뱃지를 줘라"라고 명령하지 않는다 — 즉 nutrition/exercise는 growth의 존재를 모른다.

이렇게 한 이유: 새 성장 보상(뱃지 등)을 추가할 때 발생원 도메인 코드를 건드릴 필요가 없고, 도메인 경계가 단방향으로 유지된다. 기존 `MealService`/`RoutineSessionService`의 `streakService.increment(...)` 직접 호출도 이 방향에 맞춰 이벤트로 전환한다(Nyam 초기화가 이미 동일하게 이전됨, 커밋 eb6b47a).

## 결과로 따라오는 제약

- 모든 리스너는 동기 `@EventListener`로, 발행자의 트랜잭션 안에서 실행된다 → 원자적 커밋/롤백.
- **순서 의존은 이벤트 체이닝으로 푼다.** "스트릭 N일" 같은 조건은 스트릭이 먼저 갱신돼야 하므로, StreakListener가 `MealGoalAchievedEvent`/`WorkoutLoggedEvent`를 받아 스트릭을 갱신한 뒤 **실제로 증가했을 때만** `StreakIncreasedEvent`를 발행하고, 뱃지 평가는 그걸 구독한다. (no-op 증가 시 이벤트를 내지 않아 하루 1회만 평가됨.)
