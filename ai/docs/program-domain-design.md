# Program 도메인 설계 결정

## 배경

`POST /programs` 엔드포인트는 온보딩 완료 후 회원의 식단 프로그램을 생성하는 API다.  
프로그램 생성 흐름은 다음과 같다.

```
클라이언트 → Spring Boot POST /programs
               → FastAPI POST /ai/plan/generate (내부 호출)
               ← { target_kcal, target_protein_g, target_carb_g, target_fat_g, ai_comment }
             → Program 엔티티 저장
             ← 응답 반환
```

## 설계 결정: FastAPI 응답값을 Program 엔티티에 저장

### 무엇을 추가했나

기존 `Program` 엔티티는 `targetCalories`(int)만 보유했다.  
API 응답 스펙에 따라 아래 필드를 추가했다.

```java
private double targetProteinG;
private double targetCarbG;
private double targetFatG;
private String aiComment;
```

### 왜 엔티티에 저장하는가

**대안 A — 엔티티에 저장 (채택)**

프로그램 생성 시 FastAPI 응답을 한 번 받아 DB에 함께 저장한다.

- `GET /programs/current` 조회 시 DB 한 번만 읽으면 됨
- FastAPI 재호출 없이 응답 가능 → 지연 없음, FastAPI 장애 영향 없음
- 프로그램 기간 중 목표값이 변하지 않는 불변 데이터이므로 저장이 자연스러움

**대안 B — 엔티티에 저장하지 않음**

조회 시마다 FastAPI를 호출하거나 별도 캐시 계층을 둔다.

- 조회 API마다 FastAPI 의존성 발생
- FastAPI 다운 시 조회 자체가 불가능해짐
- MVP 단계에서 캐시 계층 추가는 오버엔지니어링

### aiComment에 대한 추가 고려

`aiComment`는 비즈니스 규칙이 아닌 AI 생성 메시지라 엔티티 책임 범위 밖이라는 시각도 있다.  
별도 테이블(`ProgramAiMeta`)로 분리하면 Program 엔티티의 순수성을 유지할 수 있다.

MVP에서는 테이블 수를 최소화하기 위해 Program에 함께 저장하되,  
향후 AI 코멘트 이력 관리나 다국어 지원이 필요해지면 분리를 검토한다.

## Spring Boot TDEE 계산 제거

기존 `ProgramService`는 Spring Boot 내부에서 TDEE를 직접 계산하고 있었다.

```java
// 기존 코드
int tdee = TdeeCalculator.calculate(member.getSex(), ...);
int targetCalories = type.adjust(tdee);
```

FastAPI 연동 후에는 같은 계산을 FastAPI가 수행하므로 Spring Boot의 TDEE 계산을 제거하고  
FastAPI 응답값(`target_kcal`)을 그대로 사용한다.

`TdeeCalculator` 도메인 클래스는 삭제하지 않았다. 향후 오프라인 폴백이나 단독 계산이  
필요한 시나리오를 위해 유지한다.

## 변경 요약

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| Program 필드 | `targetCalories` | + `targetProteinG`, `targetCarbG`, `targetFatG`, `aiComment` |
| 칼로리 계산 주체 | Spring Boot (`TdeeCalculator`) | FastAPI (`/ai/plan/generate`) |
| `create()` 시그니처 | 5개 인자 | 9개 인자 |
| 서비스 반환 타입 | `void` | `ProgramResult` |
