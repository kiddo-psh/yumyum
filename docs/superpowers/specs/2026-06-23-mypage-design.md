# 마이페이지 설계 문서

**날짜:** 2026-06-23
**담당:** Spring Boot 백엔드 / Vue 프론트엔드
**관련 기능:** F202(회원 조회), F203(회원 수정), G101·G102·G104(Nyam), F302(체중 기록)

---

## 1. 개요

`/my` 라우트에 단일 스크롤 페이지로 마이페이지를 구현한다.
섹션 순서: **Nyam 캐릭터 → 프로필 → Program → 체중 → 로그아웃**

기존 DashboardView·LogView와 동일한 수직 스크롤 단일 뷰 패턴을 따른다.

---

## 2. 백엔드 추가 API

### 2-1. `GET /members/me` (신규)

`MemberController`에 메서드 추가. JWT `@AuthenticationPrincipal`로 memberId 추출.

**응답 (`MemberResponse` 재활용):**
```json
{
  "memberId": 1,
  "onboardingCompleted": true,
  "sex": "MALE",
  "birthYear": 1995,
  "heightCm": 175.0,
  "weightKg": 70.0,
  "activityLevel": "MODERATE",
  "healthGoal": "DIET"
}
```

### 2-2. `GET /nyam/status` (신규)

`GrowthController`에 메서드 추가. stateless 계산 — DB 저장 없음.

**계산 로직:**
- `DailyGoalSummaryService.todayProgress(memberId, LocalDate.now())` 호출
- `MemberService` 또는 `MemberRepository`로 `healthGoal` 조회
- `achievementRate >= 0.8` → mood `HAPPY`, `>= 0.4` → `NORMAL`, 미만 → `SAD`

**응답 (신규 `NyamStatusResponse` DTO):**
```json
{
  "mood": "HAPPY",
  "message": "오늘도 목표 달성! 냠냠이가 기뻐해요",
  "achievementRate": 0.87,
  "healthGoal": "DIET"
}
```

**mood별 메시지 예시:**
| mood | message |
|---|---|
| HAPPY | "오늘도 목표 달성! 냠냠이가 기뻐해요" |
| NORMAL | "절반쯤 왔어요. 조금만 더 힘내요!" |
| SAD | "오늘 아직 기록이 없어요. 냠냠이가 기다리고 있어요" |

### 2-3. `PATCH /members/me` (기존 재활용)

온보딩 완료 후 프로필 수정에도 동일 엔드포인트 사용.
`onboardingCompleted`가 이미 `true`인 상태에서 재호출해도 필드 덮어쓰기만 발생, 부작용 없음.

### 2-4. 기존 API (변경 없음)

| 엔드포인트 | 용도 |
|---|---|
| `GET /programs/current` | 현재 Program 조회 |
| `GET /weights` | 체중 이력 조회 |
| `POST /weights` | 체중 기록 |
| `POST /auth/logout` | 로그아웃 |

---

## 3. 프론트엔드 구조

### 3-1. 신규 파일

```
frontend/src/
├── views/
│   └── MyView.vue               # 메인 뷰 — 섹션 조합 + 병렬 데이터 로딩
├── api/
│   └── my.js                    # getMyProfile, updateMyProfile, getNyamStatus, getCurrentProgram
└── components/my/
    ├── NyamSection.vue           # 캐릭터 표시 + 상태 메시지
    ├── ProfileSection.vue        # 프로필 조회 + 인라인 수정 폼
    ├── ProgramSection.vue        # 현재 Program 정보
    └── WeightSection.vue         # 최근 체중 목록 + 입력 폼
```

### 3-2. 기존 수정 파일

- `router/index.js`: `/my` 라우트 `PlaceholderView` → `MyView`로 교체

### 3-3. MyView 데이터 로딩

마운트 시 4개 API를 `Promise.allSettled`로 병렬 호출. 일부 실패해도 나머지 섹션은 정상 렌더링.

```js
const [nyamResult, profileResult, programResult, weightsResult] = await Promise.allSettled([
  getNyamStatus(),          // api/my.js
  getMyProfile(),           // api/my.js
  getCurrentProgram(),      // api/my.js
  getWeightHistory(),       // api/dashboard.js 기존 함수
])
```

각 섹션에 `data`와 `error`를 prop으로 전달. 섹션 내부에서 에러/빈 상태 처리.

---

## 4. 섹션별 상세 설계

### 4-1. NyamSection

- mood별 배경색: `HAPPY` → `bg-nyam-mint`, `NORMAL` → `bg-yellow-200`, `SAD` → `bg-gray-200`
- mood별 애니메이션: `HAPPY` → `animate-bounce`, `NORMAL` → 없음, `SAD` → `animate-pulse`
- 이미지: Material Symbols `pets` 아이콘 (기존 MainLayout 로고와 동일 방식, 별도 에셋 없음)
- 상태 메시지 + achievementRate 퍼센트 표시

### 4-2. ProfileSection

- 조회 모드: 성별·생년·키·몸무게·활동량·HealthGoal 카드 표시
- "수정" 버튼 클릭 → 인라인 폼으로 전환 (페이지 이동 없음)
- 저장: `PATCH /members/me` → 성공 시 로컬 상태 업데이트 (재요청 없음)
- 취소: 폼 닫고 원래 값 복원

**수정 가능 필드:** sex, birthYear, heightCm, weightKg, activityLevel, healthGoal

### 4-3. ProgramSection

- `GET /programs/current` 성공 → Program 이름·기간·목표 칼로리 표시
- 404(Program 없음) → "아직 Program이 없어요" 안내 카드

### 4-4. WeightSection

- `GET /weights` → 최근 5건 목록 표시 (recordedDate, weightKg)
- 입력 폼: kg 입력 + 날짜(오늘 기본값) → `POST /weights` → 성공 시 목록 맨 앞에 추가 (재요청 없음)
- 빈 상태 → "첫 체중을 기록해보세요" 안내

### 4-5. 로그아웃

- `POST /auth/logout` → localStorage `accessToken`·`refreshToken` 삭제 → `/`로 이동

---

## 5. 에러 처리 원칙

- `Promise.allSettled` 사용: 개별 API 실패가 전체 페이지를 막지 않음
- 각 섹션 prop에 `error` 포함, 섹션 내부에서 에러 UI 렌더링
- 네트워크 오류 시 각 섹션에 재시도 버튼 제공하지 않음 (새로고침으로 대응)

---

## 6. 디자인 시스템 준수

기존 neo-brutal 디자인 토큰 사용:
- 카드: `neo-brutal-border`, `neo-brutal-shadow`
- 색상: `bg-primary`, `text-on-background`, `bg-surface`, `bg-nyam-mint`
- 타이포: `text-display-md`, `text-label-lg` (기존 토큰)
- 버튼: 기존 DashboardView·LogView 버튼 스타일 그대로 따름
