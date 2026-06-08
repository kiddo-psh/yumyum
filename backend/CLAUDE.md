# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run tests (all)
./gradlew test

# Run a single test class
./gradlew test --tests "com.ssafy.manager.nutrition.application.MealServiceTest"

# Run the app (requires MySQL + .env)
./gradlew bootRun
```

`DB_PASSWORD`는 기본값이 없으므로 `.env` 파일 또는 환경변수 필수. 나머지(`JWT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`)는 `application.properties`의 개발용 기본값으로 동작한다.

## Architecture

Spring Boot 3 / Java 21 / MySQL. 패키지 구조·도메인 책임 분리 기준은 `docs/STRUCTURE.md` 참조.

### Cross-domain references

도메인 간 참조는 plain `Long` ID만 허용 — 도메인 경계를 넘는 `@ManyToOne` / `@OneToMany` 사용 금지.

```java
private Long memberId;   // correct
// @ManyToOne Member member  — 금지
```

### Authentication

Kakao OAuth2 → JWT (stateless). `@AuthenticationPrincipal Long memberId`로 Controller에서 인증된 회원 ID를 받는다.

### Key domain rules

- **Effective date**: 새벽 04:00 전 식사 기록은 전날 `DailyGoal`에 반영된다.
- **Streak**: 식사 기록으로 `DailyGoal` 목표를 처음 초과할 때만 증가 (이미 달성 시 재증가 없음).
- **RoutineExercise 버전 관리**: AI 조정 결과는 다음 `weekNumber` 레코드를 새로 생성 (기존 레코드 수정 없음). 해당 주차 없으면 직전 주차 폴백.
- **Nyam**: stateless — 매 요청마다 `DailyGoal` 달성률 + `Member.healthGoal`로 계산. DB 저장 없음.
- **FastAPI 호출** (`/ai/routine/adjust`, `/ai/routine/generate`): `@Async`로 비동기 처리.

## REST API 규칙

URI 설계, Controller 응답, 에러 처리, 페이지네이션 규칙: `docs/rest-api.md` 참조.

## Domain glossary

전체 용어 사전: `docs/CONTEXT.md`. 혼동하기 쉬운 핵심 항목:

- **Member** — 비즈니스 레이어 공식 용어. `User`는 인증 레이어 전용.
- **DailyGoal** — "일일 미션" 아님, `daily_goals`(테이블명) 아님.
- **Meal** — `MealRecord` 아님.
- **Program** — ~~Plan~~ 아님.
- **Routine** / **RoutineExercise** / **RoutineSession** / **SessionSet** — `exercise` 단독 사용 금지.
- **Nyam** — 코드에서 "캐릭터", "마스코트" 사용 금지.
