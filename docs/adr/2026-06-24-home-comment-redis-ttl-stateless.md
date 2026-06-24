# ADR: 홈화면 AI 코멘트 Redis TTL 12h 캐싱 — DB 저장 없음
**Date:** 2026-06-24

## Context

`GET /home/comment`는 Claude API를 통해 HealthGoal 기반 개인화 코멘트를 생성한다.
매 홈화면 방문마다 Claude를 호출하면 비용·지연이 크므로 캐싱이 필요하다.

## Decision

AI 코멘트를 DB 테이블에 저장하지 않는다. Redis에 `home_comment:{memberId}` 키로 TTL 12h 저장하고,
만료 후 다음 방문 시 재생성한다. Spring `HomeCommentService`에서 Redis HIT → 즉시 반환,
MISS → FastAPI 호출 → Redis SET(TTL 12h) 로직을 인라인으로 처리한다.

## Rationale

- **코멘트는 ephemeral 데이터다.** 오늘의 동기부여 메시지를 이력으로 조회할 유즈케이스가 없다.
- **12h TTL이면 충분하다.** 하루 식단·운동 데이터가 크게 변하지 않는 주기와 맞물린다.
- **DB 스키마 불필요.** 마이그레이션 없이 Redis 설정만으로 완성 가능 — 설계 단순성 확보.
- **Redis 오류 시 자연 fallback.** 캐시 실패 → FastAPI 직접 호출로 서비스 연속성 보장.

## Alternatives Considered

- **DB 저장 (ai_home_comments 테이블):** 이력 조회 가능하지만 현재 요구사항 없음. 스키마 추가·JPA
  모델링 비용 대비 이득 없음. YAGNI.
- **앱 메모리 캐시 (Caffeine):** 단일 인스턴스에서는 동작하나 수평 확장 시 인스턴스별 캐시 불일치.
  Redis는 이미 JWT 블랙리스트용으로 인프라에 존재하므로 추가 비용 없음.
- **TTL 24h:** 하루 전체를 커버하지만 자정 이후 전날 데이터 기반 코멘트가 노출될 수 있음.
  12h로 하루 최대 2회 갱신이 더 적절한 신선도를 제공한다.

## Consequences

- Redis 장애 시 매 요청마다 FastAPI를 호출하게 된다 (캐시 건너뛰기 로직으로 서비스는 유지됨).
- 코멘트 이력 조회 기능이 필요해지면 Redis 대신 DB 저장으로 교체 필요.
- `aiHomeCommentRestClient`에 15s 전용 타임아웃을 설정했다 (Multi-Agent 체인의 60s factory와 분리).
