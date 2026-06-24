# ADR: HomeCommentService.getComment()에서 @Transactional 제거
**Date:** 2026-06-24

## Context

`HomeCommentService.getComment()`는 DB 읽기(MemberRepository, MealItemRepository 등 4개) 이후
Redis 조회 → FastAPI HTTP 호출 → Redis 저장 순서로 동작한다.
초기 구현에서 `@Transactional(readOnly = true)`를 메서드 전체에 걸었다.

## Decision

`getComment()`의 `@Transactional(readOnly = true)`를 제거한다.
`buildRequest()` 내 각 Repository 호출은 각자의 기본 전파 규칙(REQUIRED)으로 개별 트랜잭션을 가진다.

## Rationale

`@Transactional`이 메서드 전체에 걸리면 DB 커넥션이 FastAPI 응답 대기(최대 15s) + Redis 읽기/쓰기가
끝날 때까지 반납되지 않는다. Redis TTL 12h 덕분에 대부분은 HIT이지만, 동시 MISS 요청이 몰리면
커넥션 풀이 고갈될 수 있다.

DB 읽기(4개 Repository)는 순차적으로 짧게 끝나고, 일관성이 요구되는 트랜잭션 단위가 아니다.
따라서 트랜잭션 범위를 최대한 좁혀 커넥션 점유 시간을 최소화하는 것이 올바른 선택이다.

## Alternatives Considered

- **`@Transactional(readOnly = true)` 유지:** DB 읽기의 일관성을 보장하지만 커넥션이 최대 15s
  점유되어 스레드 풀 고갈 위험. 12h TTL 코멘트에 엄격한 일관성이 필요하지 않으므로 비용 대비
  효과 없음.
- **`buildRequest()`만 별도 `@Transactional` 메서드로 추출:** public 메서드만 Spring AOP 대상이므로
  `private buildRequest()`에는 적용 불가. 별도 내부 서비스 클래스로 분리해야 하는데 단일 엔드포인트를
  위한 과도한 복잡성.

## Consequences

- Repository 호출 4개가 개별 트랜잭션으로 실행되어 이론적으로 비일관성 가능 — 12h TTL 코멘트
  특성상 실질적 영향 없음.
- 향후 `buildRequest()` 내 DB 읽기가 늘어난다면 public 헬퍼 메서드 + `@Transactional(readOnly = true)`
  분리를 재고할 것.
