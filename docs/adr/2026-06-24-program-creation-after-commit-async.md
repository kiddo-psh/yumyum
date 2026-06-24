# ADR: 온보딩 완료 시 Program 생성을 AFTER_COMMIT + @Async 이벤트 리스너로 처리

**Date:** 2026-06-24

## Context

온보딩(`PATCH /members/me`)이 완료되면 Program(목표 칼로리·영양소 계획)을 자동으로 생성해야 한다.
Program 생성은 FastAPI(`/ai/plan/generate`)를 호출하며, 응답에 수 초가 걸릴 수 있다.

## Decision

`MemberOnboardedEvent`를 수신하는 `ProgramOnboardingListener`를 Spring `@Component`로 구현하고,
`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`를 함께 적용한다.

아울러 `ProgramService.create()` 내에서 Program 저장 직후 오늘 날짜의 `DailyGoal`도 함께 생성한다
(기존: 새벽 4시 배치만).

## Rationale

| 조건 | 이유 |
|---|---|
| `AFTER_COMMIT` | 온보딩 트랜잭션이 커밋된 뒤 실행되므로 FastAPI 실패 시 온보딩이 롤백되지 않는다. 동기 `@EventListener`라면 FastAPI 오류가 온보딩 PATCH 전체를 500으로 만들 수 있다. |
| `@Async` | PATCH /members/me 가 FastAPI 응답(3~5초)을 기다리지 않고 즉시 200을 반환한다. 프론트엔드가 바로 홈으로 이동할 수 있다. |
| DailyGoal 즉시 생성 | 새벽 4시 배치를 기다리면 당일 홈화면에 목표 칼로리가 표시되지 않는다. 즉시 생성으로 해결. |

## Alternatives Considered

- **동기 `@EventListener`**: FastAPI 실패 → 온보딩 롤백 → 사용자가 다시 온보딩해야 함. 거부.
- **`MemberService`에서 직접 `ProgramService.create()` 호출**: 도메인 경계 위반, 순환 의존 가능성. 거부.
- **별도 HTTP 엔드포인트(프론트가 직접 호출)**: 프론트가 Program 존재를 모르는 상태에서 타이밍 문제 발생. 거부.

## Consequences

- Program 생성 실패(FastAPI 다운 등)가 로그에만 남고 사용자에게 노출되지 않는다. 재시도 로직은 없음 — 별도 배치나 관리자 수동 재생성 필요.
- 홈화면은 `isProgramReady` 폴링(2초 × 최대 6회)으로 Program 준비 완료를 감지한다.
- `@Async`는 `AsyncConfig`(`@EnableAsync`)가 설정돼 있어야 동작한다.
