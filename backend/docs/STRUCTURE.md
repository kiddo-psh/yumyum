# 패키지 분리 가이드

---

## 1. 기본 원칙

프로젝트는 기능 코드(F101, G201 등) 기준이 아니라 **도메인 책임 기준**으로 패키지를 나눈다.

각 도메인 패키지는 아래 5개 영역으로 구성한다.

```text
{domain}
├── presentation
├── application
├── domain
└── infrastructure
    ├── persistence
    └── external
```

### Layer 역할

| Layer | 역할 |
|---|---|
| `presentation` | Controller, Request DTO, Response DTO |
| `application` | 유스케이스 서비스, 트랜잭션 처리, 여러 도메인 조합 |
| `domain` | 핵심 엔티티(@Entity 포함), 값 객체, enum, 도메인 정책 |
| `infrastructure/persistence` | Spring Data Repository |
| `infrastructure/external` | 외부 API Client, FastAPI Client, 공공데이터 API Client, FCM Client |

---

## 2. 도메인별 포함 내용

| 패키지 | 포함 도메인 / 책임 |
|---|---|
| `auth` | 로그인, 로그아웃, JWT, RefreshToken |
| `member` | 회원, 건강 프로필, 목표 유형, 신체 정보 |
| `program` | Program, DailyGoal, 목표 칼로리, 목표 영양소, 목표 달성률 |
| `nutrition` | Meal, MealItem, Food, 영양소 합산, 식단 분석, 마지막 끼니 추천 조건 |
| `exercise` | Routine, RoutineExercise, WorkoutSession, 운동 세트 기록 |
| `growth` | Streak, Nyam, Badge, Mission, 성장 보상 |
| `coaching` | FastAPI 연동, AI 식단 추천, AI 루틴 추천, AI 리포트, RAG 채팅 |
| `report` | 체중 기록, 대시보드, 주간 리포트, 누적 통계 |
| `community` | 팔로우, 게시글, 댓글, 좋아요, 식단 피드 공유 |
| `notification` | 알림, 알림 설정, FCM, 스마트 알림 |
| `global` | 공통 예외, 응답, 설정, 보안 유틸 |

---

## 3. 도메인 간 의존 규칙

### 권장

```text
Controller
→ Application Service
→ Domain
→ Repository Interface
→ Repository Implementation
→ DB
```

### 도메인 간 연결

초기에는 도메인 간 JPA 연관관계를 깊게 연결하지 않는다.

권장:

```java
private Long memberId;
private Long mealId;
private Long programId;
```

비권장:

```java
@ManyToOne
private Member member;

@OneToMany
private List<Meal> meals;
```

도메인 간 강한 JPA 연관관계는 순환 참조, Lazy Loading, 조회 성능 문제를 만들 수 있다.
