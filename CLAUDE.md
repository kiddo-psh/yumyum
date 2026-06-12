# 냠냠코치 (NyamNyam Coach) — CLAUDE.md

> Claude Code가 이 프로젝트를 작업할 때 반드시 읽어야 하는 컨텍스트 파일입니다.
> 코드 생성·수정·리뷰 전 이 파일의 내용을 먼저 숙지하세요.

---

## 1. 프로젝트 개요

**냠냠코치**는 AI 기반 영양·운동 코칭 웹 앱입니다.
사용자가 식단을 기록하면 칼로리·영양소를 분석하고, 운동 루틴을 점진적 과부하 원리로 관리하며,
Claude API를 통해 개인화된 코멘트와 식단 추천을 제공합니다.

**팀 구성:** 2인 팀
- Spring Boot 담당: 회원·식단·운동·커뮤니티·게이미피케이션 등 메인 비즈니스 로직 전체
- FastAPI 담당: AI 기능 전용 서버 (Claude API 연동, 칼로리 계산, 추세 분석)

---

## 2. 전체 아키텍처

```
[Vue 3 + Vite]
      ↓ HTTP
[Nginx :80]
      ↓
[Spring Boot :8080]  ←→  [Redis :6379]  (JWT 블랙리스트, 세션 캐시)
      ↓ 내부 HTTP         ↓
[FastAPI :8000]      [PostgreSQL :5432]  (공용 DB)
      ↓
[Claude API (Anthropic)]
```

### 핵심 원칙
- **앱(Vue)은 Spring Boot 하나만 바라봅니다.** FastAPI는 외부에 노출되지 않습니다.
- **FastAPI는 AI 기능만 담당합니다.** 비즈니스 CRUD는 Spring Boot에 있습니다.
- **DB는 공용 PostgreSQL 1개입니다.** Spring이 읽기·쓰기, FastAPI는 읽기만 합니다.
- **FastAPI URL:** Spring Boot 환경변수 `FASTAPI_INTERNAL_URL=http://fastapi:8000`으로 주입됩니다.

---

## 3. 기술 스택

| 레이어 | 기술 |
|---|---|
| 프론트엔드 | Vue 3 + Vite, Pinia, Vue Router 4, Axios, Tailwind CSS |
| 메인 백엔드 | Spring Boot 3.3, Java 21, Spring Security, JPA, Redis |
| AI 백엔드 | Python 3.11, FastAPI, uvicorn, SQLAlchemy, Alembic |
| AI | Claude API (`claude-haiku-4-5` 기본, `claude-opus-4-5` RAG/Multi-Agent) |
| DB | PostgreSQL 16, Redis 7 |
| 인프라 | Docker Compose, Nginx |

### Claude 모델 사용 기준
- **`claude-haiku-4-5`**: 일반 AI 기능 (플랜 생성, 식단 추천, 루틴 조정 코멘트 등)
- **`claude-opus-4-5`**: RAG 영양 채팅(F703), Multi-Agent 코칭(F704) — 크레딧 소모 주의

---

## 4. 폴더 구조

```
nyamnyam/
├── docker-compose.yml
├── .env.example
├── .gitignore
├── nginx/
│   └── nginx.conf
│
├── spring-server/                     # Spring Boot 메인 서버
│   ├── Dockerfile
│   └── src/main/java/com/nyamnyam/
│       ├── NyamnyamApplication.java
│       ├── domain/
│       │   ├── auth/                  # 회원가입·로그인·JWT
│       │   ├── member/                # 회원 CRUD
│       │   ├── meal/                  # 식단 CRUD + 영양소 합산
│       │   ├── food/                  # 음식 DB (식품안전처 연동)
│       │   ├── program/               # Program + DailyGoal
│       │   ├── streak/                # Streak + 마일스톤
│       │   ├── weight/                # 체중 기록
│       │   ├── routine/               # 운동 루틴 (F901~F903)
│       │   ├── community/             # 팔로우·게시판·댓글·피드
│       │   ├── badge/                 # 뱃지 시스템
│       │   ├── nyam/                  # Nyam 캐릭터 상태
│       │   └── ai/                    # FastAPI 호출 전담
│       │       ├── controller/        # AI 기능 요청 진입점
│       │       ├── service/           # WebClient로 FastAPI 호출
│       │       └── dto/               # 요청·응답 DTO (FastAPI와의 계약)
│       └── global/
│           ├── config/                # Security, WebClient, CORS 설정
│           ├── exception/             # 공통 예외 처리
│           └── jwt/                   # JWT 발급·검증
│
├── fastapi-server/                    # FastAPI AI 전용 서버
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── main.py                    # 앱 진입점
│       ├── config.py                  # .env 로딩 (Settings)
│       ├── api/
│       │   └── v1/
│       │       ├── router.py          # 전체 라우터 통합
│       │       ├── plan.py            # /ai/plan/*
│       │       ├── routine.py         # /ai/routine/*
│       │       ├── meal.py            # /ai/meal/*
│       │       └── report.py          # /ai/report/*, /ai/checkin/*
│       ├── services/
│       │   ├── claude_service.py      # Claude API 호출 단일 진입점
│       │   ├── plan_service.py        # BMR·TDEE 계산 (순수 함수)
│       │   ├── routine_service.py     # 점진적 과부하 로직
│       │   └── trend_service.py       # 체중 추세 분석 (numpy)
│       ├── schemas/                   # Pydantic 요청·응답 모델
│       └── db/
│           ├── database.py            # AsyncSession 설정
│           └── models.py              # SQLAlchemy 모델 (읽기 전용)
│
└── vue-frontend/                      # Vue 3 프론트엔드
    ├── Dockerfile
    ├── src/
    │   ├── api/                       # Axios API 함수
    │   ├── stores/                    # Pinia 상태 관리
    │   ├── router/                    # Vue Router + 인증 가드
    │   ├── views/                     # 페이지 컴포넌트
    │   └── components/                # 재사용 UI 컴포넌트
    └── vite.config.js
```

---

## 5. 도메인 용어 사전 (Bounded Context Glossary)

코드 작성 시 아래 용어를 정확히 사용하세요. 잘못된 용어 사용은 팀 내 혼선을 유발합니다.

| 용어 | 설명 | ❌ 잘못된 표현 |
|---|---|---|
| **Member** | 앱 사용자. 비즈니스 레이어 공식 용어 | ~~User~~ (기술 레이어 전용) |
| **Program** | 기간·목표 기반 AI 생성 건강 관리 과정 | ~~Plan~~, ~~WeeklyPlan~~ |
| **WeeklyReport** | Program 내 매 7일 AI 생성 피드백 | ~~ai_report~~, ~~주간 플랜~~ |
| **DailyGoal** | 하루 단위 달성 목표. `achieved` / `not achieved` | ~~일일 미션~~ (게이미피케이션 용어와 혼동) |
| **Streak** | DailyGoal 연속 달성 일수 | ~~연속 달성일~~ |
| **HealthGoal** | 온보딩 시 선택하는 상위 목표 | ~~goal~~ (단독 사용 시 범위 불명확) |
| **Food** | 영양 정보 마스터 데이터 단위 | ~~FoodDB~~, ~~FoodItem~~ |
| **Meal** | 한 끼 식사 기록 결과물 | ~~MealRecord~~ |
| **MealItem** | Meal에 포함된 음식·섭취량 쌍 | - |
| **Nyam** | 마스코트 캐릭터 | ~~캐릭터~~, ~~마스코트~~ |

### HealthGoal 값
```
DIET     → 다이어트
MUSCLE   → 근육 증가
HEALTH   → 건강 유지
DISEASE  → 질환 관리
```

---

## 6. 4주 개발 로드맵

### 1주차 — 1차 MVP: 핵심 뼈대
**완료 기준:** 가입 → 온보딩 → 식단 기록 → Streak 조회 흐름 동작

**Spring Boot 작업**
- [ ] Docker Compose 전체 실행 확인
- [ ] Spring Boot 프로젝트 생성, JWT 필터 구조
- [ ] `domain/auth`: F201 회원가입, F205 로그인·로그아웃 (JWT + Redis)
- [ ] `domain/member`: F202/203/204 조회·수정·비활성화(`INACTIVE`)
- [ ] `domain/meal`: F101~F105 Meal CRUD + 영양소 합산
- [ ] `domain/program`: 온보딩 완료 시 Program 생성 (FastAPI 호출)
- [ ] `domain/streak`: G201 Streak 카운트, G203 알림 트리거
- [ ] `domain/nyam`: G101/G102/G104 Nyam 상태 반환

**FastAPI 작업**
- [ ] `app/` 폴더 구조 정리, `/health` 엔드포인트
- [ ] `config.py`: `.env` 로딩, Claude 클라이언트 초기화
- [ ] `claude_service.py`: mock 모드 구현 (API 키 없는 개발 환경)
- [ ] `POST /ai/plan/generate`: BMR·TDEE 계산 + Claude 코멘트
- [ ] `GET /food/search`: 식품안전처 API 연동

---

### 2주차 — 1차 마무리 + 2차 시작: 운동 루틴 + 마지막 끼니 추천
**완료 기준:** 운동 세션 기록 → AI 루틴 조정 반환, 마지막 끼니 추천 3개 JSON 반환

**Spring Boot 작업**
- [ ] G501 주간 달성 캘린더, G502 목표 달성 진행바
- [ ] `domain/routine`: F901 루틴 등록, F903 세션 기록
- [ ] 세션 완료 시 `domain/ai/service` → FastAPI `/ai/routine/adjust` 자동 호출
- [ ] F801 칼로리 밸런스 연동 (운동 소모 → 목표 칼로리 재계산)
- [ ] F802 마지막 끼니 추천 트리거 조건 체크 (잔여칼로리 1끼 ±20% + 오후 5시 이후)
- [ ] `domain/weight`: F302 체중 기록 CRUD

**FastAPI 작업**
- [ ] `POST /ai/routine/generate`: 루틴 없는 사용자 AI 추천, 기존 루틴 점진 계획
- [ ] `POST /ai/routine/adjust`: UP/HOLD/DOWN/VOLUME_UP/DELOAD 판단
- [ ] `GET /ai/routine/weekly-plan/{id}/{week}`: 주차별 루틴 조회
- [ ] `POST /ai/meal/last-recommend`: 잔여 영양소 분석 → 식단 3개 추천
- [ ] `POST /ai/report/weekly`: 7일 데이터 분석, numpy 추세 계산
- [ ] `POST /ai/plan/weekly-adjust`: 체중 추세 기반 칼로리 재조정

---

### 3주차 — 2차 MVP: 커뮤니티 + AI 코칭
**완료 기준:** 게시판 E2E, AI 식단 분석 코멘트 반환

**Spring Boot 작업**
- [ ] `domain/community`: F401 팔로우, F402 게시판, F403 댓글, F404 피드, F405 템플릿
- [ ] `domain/badge`: G601 뱃지 자동 지급, G602 컬렉션 조회, G603 알림
- [ ] `domain/nyam`: G103 스킨 해금 (Streak·뱃지 달성 시)
- [ ] F303 통합 대시보드 API

**FastAPI 작업**
- [ ] `POST /ai/diet/analyze`: 영양 균형 분석 (F701)
- [ ] `POST /ai/exercise/coach`: 루틴 기반 맞춤 코칭 (F702)
- [ ] `POST /ai/checkin/biweekly`: 2주 달성률 분석, 목표 조정 옵션 (F804)

---

### 4주차 — 3차 MVP: AI 고도화 + 통합 QA
**완료 기준:** docker compose up 한 번으로 전체 실행·데모 가능

**Spring Boot 작업**
- [ ] G401 일일 미션, G402 전체 완료 보너스
- [ ] G201 Streak 마일스톤 보상 (3·7·30·100일)
- [ ] G503 누적 통계 API
- [ ] 필수 기능 전체 E2E 테스트, Spring ↔ FastAPI 연동 7개 포인트 전수 확인

**FastAPI 작업**
- [ ] `POST /ai/chat`: RAG 기반 영양 Q&A, pgvector 벡터 DB 구축 (F703)
- [ ] Multi-Agent 오케스트레이션 (영양·운동·알림 Agent, F704)
- [ ] `POST /ai/mission/generate`: 개인화 미션 생성 (G403)
- [ ] `POST /ai/notification/timing`: 스마트 알림 타이밍 (F705)
- [ ] 전체 엔드포인트 pytest 작성

---

## 7. Spring ↔ FastAPI 연동 포인트 7개

> 이 7개가 두 서버의 경계면입니다. DTO 구조가 바뀌면 **반드시 양쪽 동시 수정** 필요.

| # | Spring 호출 시점 | FastAPI 엔드포인트 | 방향 |
|---|---|---|---|
| 1 | 온보딩 완료 시 | `POST /ai/plan/generate` | Spring → FastAPI |
| 2 | 운동 세션 완료 시 | `POST /ai/routine/adjust` | Spring → FastAPI |
| 3 | 루틴 등록 시 | `POST /ai/routine/generate` | Spring → FastAPI |
| 4 | 칼로리 잔여 조건 충족 시 | `POST /ai/meal/last-recommend` | Spring → FastAPI |
| 5 | Program 7일 경과 시 | `POST /ai/report/weekly` | Spring → FastAPI |
| 6 | WeeklyReport 생성 후 | `POST /ai/plan/weekly-adjust` | Spring → FastAPI |
| 7 | 2주 달성률 50% 미만 시 | `POST /ai/checkin/biweekly` | Spring → FastAPI |

### Spring Boot WebClient 호출 패턴
```java
// domain/ai/service/AiPlanService.java
@Service
@RequiredArgsConstructor
public class AiPlanService {

    private final WebClient webClient;

    @Value("${fastapi.internal-url}")
    private String fastapiUrl;

    public AiPlanResponse generatePlan(OnboardingData data) {
        return WebClient.create(fastapiUrl)
            .post()
            .uri("/ai/plan/generate")
            .bodyValue(data)
            .retrieve()
            .bodyToMono(AiPlanResponse.class)
            .block();
    }
}
```

### FastAPI 수신 패턴
```python
# app/api/v1/plan.py
@router.post("/ai/plan/generate", response_model=PlanResponse)
async def generate_plan(data: OnboardingData):
    kcal = plan_service.calc_tdee(data)          # 공식 계산 (Claude 불필요)
    comment = await claude_service.generate(...)  # Claude API
    return PlanResponse(daily_kcal=kcal, ai_comment=comment, ...)
```

---

## 8. FastAPI 핵심 설계 규칙

### claude_service.py 사용 원칙
- **모든 Claude API 호출은 `claude_service.py` 한 곳에서만** 이루어집니다.
- 엔드포인트 함수 안에 직접 `claude.messages.create()`를 호출하지 마세요.
- mock 모드: `ENV=dev`일 때 Claude 호출 없이 더미 응답 반환 → API 키 없이 개발 가능

```python
# app/services/claude_service.py
class ClaudeService:
    def __init__(self):
        self.client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
        self.mock_mode = settings.env == "dev"

    async def generate_comment(self, prompt: str) -> str:
        if self.mock_mode:
            return "[MOCK] AI 코멘트가 여기에 표시됩니다."
        message = self.client.messages.create(
            model="claude-haiku-4-5",
            max_tokens=300,
            messages=[{"role": "user", "content": prompt}]
        )
        return message.content[0].text
```

### plan_service.py — 칼로리 계산 (Claude 불필요)
```python
# BMR: Mifflin-St Jeor 공식
# 남성: 10W + 6.25H - 5A + 5
# 여성: 10W + 6.25H - 5A - 161
# TDEE = BMR × 활동량 계수 (1.2 / 1.375 / 1.55 / 1.725)
# 목표 칼로리 = TDEE ± 400 kcal
# 하한선: 여성 1200 kcal, 남성 1500 kcal (절대 이 이하로 설정 금지)
```

### routine_service.py — 루틴 조정 기준
```
모든 세트·반복 성공       → UP        무게 +2.5~5 kg
일부 세트만 성공          → HOLD      무게 유지
2주 연속 실패             → DOWN      무게 -5~10%
3주 연속 성공             → VOLUME_UP 세트 +1 or 무게 +5 kg
4주 누적 피로 감지        → DELOAD    무게 -40%, 세트 -1
```

---

## 9. F802 마지막 끼니 추천 — 상세 로직

```
트리거 조건 (Spring이 판단):
  1. 잔여 칼로리 = 목표칼로리 ÷ 끼니수 × (0.8 ~ 1.2) 범위 내
  2. 현재 시각 >= 오후 17:00
  3. 당일 Meal 기록 >= 1건

FastAPI 처리 순서:
  1. 잔여 영양소 계산
     remain_protein = target_protein - total_protein
     remain_carb    = target_carb    - total_carb
     remain_fat     = target_fat     - total_fat
  2. 부족 비율 가장 높은 영양소 → priority_nutrient 결정
  3. Claude API 프롬프트: 잔여 영양소 그램 수 포함, 한식 기반 1끼 분량 3개 추천
  4. 응답: recommendations(3개) + priority_nutrient + ai_comment
```

---

## 10. 환경 변수

### 루트 `.env` (Docker Compose용)
```
DB_USER=nyamnyam
DB_PASSWORD=...
JWT_SECRET=...              # 32자 이상
ANTHROPIC_API_KEY=sk-ant-...
FOOD_DB_API_KEY=...         # 식품안전처 공공데이터포털
```

### FastAPI `.env`
```
DATABASE_URL=postgresql+asyncpg://...
ANTHROPIC_API_KEY=sk-ant-...
FOOD_DB_API_KEY=...
ENV=dev                     # dev: mock 모드 / prod: 실제 Claude 호출
```

### Vue `.env.local`
```
VITE_API_BASE_URL=http://localhost/api
```

---

## 11. 실행 방법

```bash
# 전체 실행
cp .env.example .env       # 값 채우기
docker compose up -d

# 개발 중 개별 실행
cd fastapi-server && uvicorn app.main:app --reload   # FastAPI
cd vue-frontend && npm run dev                        # Vue (localhost:5173)

# 재빌드 (코드 변경 시)
docker compose up -d --build fastapi
docker compose up -d --build spring
```

### 동작 확인
```
FastAPI Swagger: http://localhost:8000/docs  (fastapi 포트 임시 오픈 시)
Spring Swagger:  http://localhost:8080/swagger-ui.html
Vue 앱:          http://localhost (nginx 경유)
```

---

## 12. 코드 작성 규칙

### 공통
- 커밋 메시지: `feat(meal): F101 식단 작성 API 구현`
- 브랜치: `feature/F101-meal-create`, `feature/F802-last-meal-recommend`
- `.env` 파일은 절대 git에 커밋하지 않습니다.

### Spring Boot
- 패키지: `com.nyamnyam.domain.{도메인명}.{레이어명}`
- 예외 처리: `global/exception/` 아래 공통 핸들러 사용
- FastAPI 호출: `domain/ai/service/` 아래에만 위치. 다른 도메인이 FastAPI를 직접 알지 않도록 합니다.
- 응답 래퍼: `ApiResponse<T>` 공통 응답 포맷 사용

#### DTO 클래스명 규칙
레이어 경계를 기준으로 접미사를 구분한다.

| 레이어 | 역할 | 접미사 | 예시 |
|---|---|---|---|
| `presentation/dto/` | HTTP 요청 바디 | `{기능}Request` | `MealRequest` |
| `presentation/dto/` | HTTP 응답 바디 | `{기능}Response` | `MealResponse` |
| `application/` | 서비스 메서드 입력 | `{기능}Command` | `MealCommand` |
| `application/` | 서비스 메서드 반환 | `{기능}Result` | `ProgramResult` |
| `infrastructure/client/` | 외부 서비스 송신 | `{기능}ClientRequest` | `AiPlanClientRequest` |
| `infrastructure/client/` | 외부 서비스 수신 | `{기능}ClientResponse` | `AiPlanClientResponse` |

- presentation DTO는 항상 `presentation/dto/` 패키지에 위치한다. `presentation/` 직접 위치는 금지.

#### 레이어 의존 방향
- `application` 레이어 서비스는 `presentation` DTO를 반환하지 않는다.
- 서비스는 `application` 레이어 전용 Result/DTO를 반환하고, 컨트롤러에서 presentation DTO로 변환한다.
  ```java
  // ❌ 금지
  public LastMealRecommendResponse lastRecommend(...) { ... }

  // ✅ 올바른 방향
  public AiMealLastRecommendResult lastRecommend(...) { ... }  // application DTO 반환
  // 컨트롤러에서: ResponseEntity.ok(LastMealRecommendResponse.from(service.lastRecommend(...)))
  ```

#### URL 설계
- `/ai/**`는 Spring → FastAPI 내부 호출 경로 전용으로 예약한다.
- 사용자(Vue) → Spring 요청은 리소스 기반 URL을 사용한다. AI 기능 여부는 URL에 드러내지 않는다.
  ```
  ❌ POST /ai/meals/last-recommend   (Vue → Spring)
  ✅ POST /meals/last-recommend      (Vue → Spring)
  ✅ POST /ai/meal/last-recommend    (Spring → FastAPI 내부)
  ```

#### HTTP 응답 방식
- 컨트롤러 응답은 `ResponseEntity`로 통일한다. `@ResponseStatus` + DTO 직접 반환 방식은 사용하지 않는다.
  ```java
  // ❌ 금지
  @ResponseStatus(HttpStatus.CREATED)
  public RoutineResponse createManual(...) { return ...; }

  // ✅ 올바른 방식
  public ResponseEntity<RoutineResponse> createManual(...) {
      return ResponseEntity.status(HttpStatus.CREATED).body(...);
  }
  ```

#### 메서드 단일 책임
- 하나의 메서드가 저장·외부 호출·변환 등 여러 역할을 동시에 가지지 않는다.
- 역할이 둘 이상이면 private 메서드로 추출해 각자 하나의 역할만 담당하게 한다.

### FastAPI
- 모든 Claude 호출은 `claude_service.py` 경유
- 순수 계산 함수(BMR, TDEE, 루틴 조정)는 서비스 레이어에 분리 (테스트 용이성)
- Pydantic 모델은 `schemas/` 폴더에 도메인별로 분리
- 라우터는 `api/v1/router.py`에서 통합 등록
- 비동기 DB 접근: `AsyncSession` 사용 (동기 혼용 금지)

### Vue
- API 함수: `src/api/{도메인}.js`에 모아두기
- 전역 상태: Pinia store. 컴포넌트에서 직접 `axios` 호출 금지
- 컴포넌트 파일명: PascalCase (`MealCard.vue`)
- 환경변수: `import.meta.env.VITE_*` 형태

---

## 13. 기능 우선순위 요약

| 우선순위 | 기능 번호 | 기능명 | 담당 |
|---|---|---|---|
| **필수** | F101~F105 | Meal CRUD + 분석 | Spring |
| **필수** | F201~F206 | Member + 온보딩 AI 플랜 | Spring + FastAPI |
| **필수** | F302 | 체중 기록 | Spring |
| **필수** | F801 | 칼로리 밸런스 연동 | Spring |
| **필수** | F802 | 마지막 끼니 추천 | FastAPI |
| **필수** | F901~F904 | 운동 루틴 시스템 | Spring + FastAPI |
| **필수** | G101·G102·G104 | Nyam 외형·메시지 | Spring |
| **필수** | G201·G203 | Streak | Spring |
| **필수** | G401·G402 | 일일 미션 | Spring |
| **필수** | G501·G502 | 달성 시각화 | Spring |
| **추가** | F303·F304 | 대시보드·주간 리포트 | Spring + FastAPI |
| **추가** | F401~F405 | 커뮤니티 | Spring |
| **추가** | F804 | 2주 AI 체크인 | FastAPI |
| **추가** | G601~G603 | 뱃지 | Spring |
| **심화** | F703 | RAG 영양 채팅 | FastAPI |
| **심화** | F704 | Multi-Agent 코칭 | FastAPI |
| **심화** | F701·F702 | AI 식단·운동 분석 | FastAPI |
| **심화** | F705·G403 | 스마트 알림·개인화 미션 | FastAPI |

---

## 14. 자주 묻는 질문 (FAQ for Claude Code)

**Q. Spring Boot에서 FastAPI를 어떻게 호출하나요?**
`domain/ai/service/` 아래 서비스 클래스에서 `WebClient`를 사용합니다.
URL은 `FASTAPI_INTERNAL_URL` 환경변수로 주입받습니다. Docker 네트워크 내에서 `http://fastapi:8000`으로 연결됩니다.

**Q. Claude API 키 없이 개발할 수 있나요?**
네. FastAPI의 `ENV=dev` 설정 시 `claude_service.py`가 mock 응답을 반환합니다.
Spring Boot와 Vue는 Claude 키와 무관하게 동작합니다.

**Q. DB 마이그레이션은 어떻게 하나요?**
Spring Boot: JPA DDL auto 또는 Flyway 사용
FastAPI: Alembic 사용 (`alembic upgrade head`)
FastAPI는 DB를 읽기 전용으로만 사용하므로 마이그레이션은 Spring 쪽에서 관리합니다.

**Q. Coin 시스템이 있나요?**
없습니다. v3에서 전면 삭제했습니다. 동기부여는 Streak·뱃지·Nyam 성장으로만 표현합니다.

**Q. XP·레벨 시스템이 있나요?**
없습니다. v3에서 삭제했습니다. 성장 지표는 뱃지 컬렉션과 Nyam 캐릭터 외형 변화로 대체합니다.

**Q. 운동 루틴 조정(F904)은 언제 발동되나요?**
`POST /routines/sessions` 호출(운동 세션 기록) 시 Spring이 자동으로 FastAPI `/ai/routine/adjust`를 호출합니다.
별도 트리거 없이 세션 기록 → 자동 조정이 이루어집니다.

**Q. 마지막 끼니 추천(F802)은 언제 발동되나요?**
Spring이 `GET /dashboard/calories-balance` 응답에 `lastMealRecommendTrigger: true`를 포함할 때
Vue 프론트가 `POST /ai/meal/last-recommend`를 호출합니다.
조건: 잔여칼로리 1끼 분량 ±20% + 오후 17시 이후 + 당일 1끼 이상 기록.
