# 주간 리포트(WeeklyReport) 프론트 화면 + 평가 트리거 설계

작성일: 2026-06-25
작성자: g0rnn
브랜치: feature/ai-report

## 1. 배경 / 목적

AI 주간 리포트(F304/F704) 기능을 **프론트 결과 화면까지 포함해 E2E로 평가**한다.
현재 백엔드(`GET /programs/{id}/weekly-reports/{week}`)와 FastAPI 멀티에이전트 코칭
(`/ai/coaching/weekly`)은 완성돼 있으나, 프론트 `report` 라우트가
`PlaceholderView`(빈 화면)이고 리포트 조회 API·뷰 컴포넌트가 없다.

따라서 이번 작업은 두 가지다.
1. 리포트를 화면에 그리는 **프론트 목록+상세 화면 신규 구현**
2. 평가를 위해 리포트를 즉시 생성하는 **임시 dev 트리거** (평가 후 제거)

## 2. 현재 동작 흐름 (참고)

```
DailyBatchJob (@Scheduled 04:00)
  └ weeklyReportService.createStubs(today)
       ├ ACTIVE 프로그램 중 start_date 로부터 7일 이상 경과 건 선별
       ├ weekNumber = 경과일 / 7
       ├ weekly_report stub 저장
       └ WeeklyCoachingDataService.buildRequest(programId, weekNumber)
            → AiCoachingClient.weekly() → FastAPI POST /ai/coaching/weekly
            → 응답(ai_comment, nutrition/exercise/goal_summary,
                   avg_calorie_rate, achievement_days, weight_trend)을 stub.fill
조회: GET /programs/{programId}/weekly-reports/{weekNumber}  (단건, 소유권 검증)
```

리포트 데이터 소스 테이블: `program`(목표·기간), `meal`/`meal_item`(effective_date 집계),
`weight`(추세), `routine_session`/`session_set`(운동).

## 3. 설계 결정

| 항목 | 결정 |
|---|---|
| 화면 범위 | 리포트 **목록 + 상세** |
| 화면 구조 | 별도 라우트 (`report` 목록 / `report/:weekNumber` 상세) — 기존 `routine`→`routine/:routineId` 패턴 답습 |
| 목록 데이터 | 백엔드 **목록 조회 API 신규 추가** (프론트 주차 반복 호출 대신) |
| 생성 트리거 | **임시 dev 엔드포인트** (`POST /dev/weekly-reports/run`), 평가 후 제거 |

## 4. 백엔드 변경

### 4.1 목록 조회 API (정식)

- `WeeklyReportRepository`
  ```java
  List<WeeklyReport> findByProgramIdOrderByWeekNumberAsc(Long programId);
  ```
- `WeeklyReportController`
  ```
  GET /programs/{programId}/weekly-reports
  ```
  - `@AuthenticationPrincipal Long memberId` 로 소유권 검증
    (`program.getMemberId().equals(memberId)` 아니면 `ForbiddenException`)
  - `List<WeeklyReportResponse>` 반환 (기존 `WeeklyReportResponse.from` 재사용)
  - 프로그램 없음 → `NoSuchElementException` (기존 단건 핸들러와 동일)

### 4.2 임시 dev 트리거 (평가용, 제거 예정)

- 신규 `DevWeeklyReportController` (또는 global/dev 패키지)
  ```
  POST /dev/weekly-reports/run?date=YYYY-MM-DD
    → weeklyReportService.createStubs(date)
    → 200 OK
  ```
  - `date` 미지정 시 `LocalDate.now()`
  - 클래스 상단 `// TEMP: 주간 리포트 평가용. 평가 후 이 컨트롤러와 SecurityConfig permitAll 제거`
- `SecurityConfig`: `.requestMatchers("/dev/**").permitAll()` 한 줄 추가 (임시, 로컬 평가 전용)
- 평가 종료 후 제거 대상: `DevWeeklyReportController`, `/dev/**` permitAll 라인

> 주의: `createStubs(date)` 의 `date` 는 데이터의 `start_date(2026-06-18) + 7일` 이상이어야
> weekNumber=1 리포트가 생성된다. 평가 시 `date=2026-06-25` 사용.

## 5. 프론트엔드 변경

### 5.1 API — `src/api/report.js` (신규)

```js
import { apiClient } from '@/services/apiClient';

export function getWeeklyReports(programId) {
  return apiClient.get(`/programs/${programId}/weekly-reports`);
}

export function getWeeklyReport(programId, weekNumber) {
  return apiClient.get(`/programs/${programId}/weekly-reports/${weekNumber}`);
}
```
프로그램 정보는 기존 `getCurrentProgram()` (`src/api/my.js`) 재사용.

### 5.2 라우트 — `src/router/index.js`

- `report` 라우트의 `component`를 `PlaceholderView` → `ReportView`로 교체
- `report/:weekNumber` 신규 (name: `report-detail`, component: `ReportDetailView`)
  — `routine/:routineId` 등록 방식과 동일

### 5.3 뷰 컴포넌트 (신규 2개)

**`ReportView.vue` (목록)**
- onMounted: `getCurrentProgram()` → `programId` → `getWeeklyReports(programId)`
- 주차별 카드 렌더: `weekNumber`, `avgCalorieRate`, `achievementDays` 미리보기
- 카드 탭 → `router.push({ name: 'report-detail', params: { weekNumber } })`

**`ReportDetailView.vue` (상세)**
- 라우트 param `weekNumber`
- onMounted: `getCurrentProgram()` → `programId` → `getWeeklyReport(programId, weekNumber)`
  (라우트 직접 진입/새로고침 대비해 상세에서도 programId 재조회)
- 렌더 8필드:
  - 상단 통계 카드: `avgCalorieRate`(%), `achievementDays`(N/7일), `weightTrend`(±kg/주, null이면 "기록 없음")
  - `content` (종합 코멘트)
  - 섹션 카드 3개: `nutritionSummary`(영양) / `exerciseSummary`(운동) / `goalSummary`(목표)

### 5.4 상태 처리

| 상태 | 목록 | 상세 |
|---|---|---|
| 로딩 | 스피너/스켈레톤 | 〃 |
| 프로그램 없음 | "진행 중인 프로그램이 없어요" | 〃 |
| 리포트 없음(빈 배열) | "아직 생성된 주간 리포트가 없어요" | — |
| 404 | — | "해당 주차 리포트가 없어요" |
| 기타 에러 | 재시도 안내 | 〃 |

### 5.5 레이아웃 (모바일)

```
목록 (ReportView)               상세 (ReportDetailView)
┌────────────────────────┐      ┌────────────────────────────┐
│ 주간 리포트             │      │ ← 1주차 리포트              │
│ ┌────────────────────┐ │      │ ┌────────────────────────┐ │
│ │ 1주차      [달성5/7]│ │      │ │ 칼로리 99% · 달성 5/7일 │ │
│ │ 평균 칼로리 99%  〉 │ │      │ │ 체중 -1.0kg/주          │ │
│ └────────────────────┘ │      │ └────────────────────────┘ │
│ ┌────────────────────┐ │      │ 💬 종합 코멘트 (content)    │
│ │ 2주차 ...           │ │      │ ─────────────────────────  │
│ └────────────────────┘ │      │ 🥗 영양 (nutritionSummary)  │
└────────────────────────┘      │ 🏋️ 운동 (exerciseSummary)   │
                                 │ 🎯 목표 (goalSummary)       │
                                 └────────────────────────────┘
```
스타일은 기존 뷰(DashboardView/RoutineHistoryView)의 Tailwind 카드 패턴을 따른다.

## 6. E2E 평가 절차

1. 더미데이터 삽입 완료 (member_id=1, program start_date=2026-06-18, 7일치 식단/체중/운동)
2. FastAPI 가동 (`ENV=prod` → 실제 Claude 4콜 발생) + Spring 가동
3. `POST /dev/weekly-reports/run?date=2026-06-25` 호출 → weekly_report 생성
4. 프론트 로그인(카카오, member 1) → 하단 네비 `리포트` → 목록에 1주차 카드 확인
5. 카드 탭 → 상세에서 통계·종합·영양/운동/목표 코멘트 렌더 확인
6. 평가 종료 후 임시 dev 트리거(컨트롤러 + permitAll) 제거

## 7. 범위 밖 (YAGNI)

- 정식 "리포트 생성" 버튼/재생성 기능 (이번엔 임시 트리거로 갈음)
- 주차 간 슬라이드/스와이프 네비게이션 (목록→상세 라우트로 충분)
- 페이지네이션 (한 프로그램의 주차 수는 적음)
