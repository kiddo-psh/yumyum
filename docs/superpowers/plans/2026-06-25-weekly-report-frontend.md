# 주간 리포트 프론트 화면 + 평가 트리거 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI 주간 리포트(WeeklyReport)를 프론트에서 목록+상세로 조회하는 화면을 만들고, 평가용 임시 생성 트리거를 추가해 E2E로 검증한다.

**Architecture:** 백엔드에 리포트 목록 조회 API(정식)와 평가용 임시 dev 생성 트리거를 추가하고, 프론트에 `report.js` API + `ReportView`(목록)/`ReportDetailView`(상세) 두 컴포넌트를 추가해 기존 `routine`→`routine/:routineId` 라우트 패턴을 답습한다.

**Tech Stack:** Spring Boot 3 / Java 21 / Spring Security(WebMvcTest) · Vue 3 + Vite + Tailwind(neo-brutal 디자인 토큰) · 커스텀 `apiClient`(fetch 래퍼, 파싱된 JSON 바디 직접 반환)

## Global Constraints

- 도메인 용어: **Member**(User 아님), **Program**(Plan 아님), **WeeklyReport**(주간 플랜 아님). 코드·주석에서 정확히 사용.
- 도메인 간 참조는 plain `Long` ID만. 경계 넘는 `@ManyToOne`/`@OneToMany` 금지.
- Spring Controller 응답은 `ResponseEntity`로 통일. `@ResponseStatus`+DTO 직접 반환 금지.
- presentation DTO는 `presentation/dto/` 패키지. 기존 `WeeklyReportResponse`(presentation/dto)와 `WeeklyReportResponse.from` 재사용.
- 인증: `@AuthenticationPrincipal Long memberId`. 소유권 검증 실패 시 `ForbiddenException`(→403), 미존재 시 `NoSuchElementException`(→404). 두 핸들러는 `GlobalExceptionHandler`에 이미 존재.
- 프론트 API 호출은 `src/api/{도메인}.js`에 모은다. 컴포넌트에서 직접 fetch/axios 금지. `apiClient.get(...)`은 파싱된 응답 바디를 그대로 resolve한다.
- 프론트 컴포넌트 파일명 PascalCase. Tailwind 디자인 토큰은 기존 뷰(`RoutineHistoryView`)의 `neo-brutal-border rounded-2xl bg-white`, `text-display-md`, `text-on-background`, `text-on-surface-variant`, `material-symbols-outlined`, `bg-surface`, `nyam-mint` 등을 따른다.
- 프론트 자동화 테스트 인프라 없음(vitest/test-utils 미설치) → 프론트 작업은 **브라우저 수동 검증**으로 대체한다. 백엔드 작업은 기존 `@WebMvcTest` 패턴으로 TDD.
- 커밋 메시지 컨벤션: `feat(scope): ...`, `docs: ...` 등. 한국어 본문 허용.

---

### Task 1: 백엔드 — 리포트 목록 조회 API

**Files:**
- Modify: `backend/src/main/java/com/ssafy/manager/program/infrastructure/persistence/WeeklyReportRepository.java`
- Modify: `backend/src/main/java/com/ssafy/manager/program/presentation/WeeklyReportController.java`
- Test: `backend/src/test/java/com/ssafy/manager/program/presentation/WeeklyReportControllerTest.java` (기존 파일에 케이스 추가)

**Interfaces:**
- Consumes: `WeeklyReportResponse.from(WeeklyReport)` (기존), `ProgramRepository.findById`, `ForbiddenException`
- Produces: `GET /programs/{programId}/weekly-reports` → `List<WeeklyReportResponse>` (weekNumber 오름차순). 프론트 `getWeeklyReports`가 소비.

- [ ] **Step 1: 실패하는 테스트 추가**

`WeeklyReportControllerTest.java`의 클래스 안에 아래 import와 테스트 3개를 추가한다.
파일 상단 import 영역에 다음을 추가:
```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
```
(이미 `MockMvcResultMatchers.*` 와일드카드 import가 있으면 생략 가능)

클래스 내부에 테스트 추가:
```java
    @Test
    void 리포트_목록을_주차_오름차순으로_조회한다() throws Exception {
        Program program = Program.create(MEMBER_ID, ProgramType.DIET, LocalDate.now(), LocalDate.now().plusDays(27),
                1800, 130.0, 180.0, 50.0, null);
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(program));

        WeeklyReport week1 = new WeeklyReport(PROGRAM_ID, 1);
        week1.fill("코멘트1", "영양1", "운동1", "목표1", 99.0, 5, -1.0);
        WeeklyReport week2 = new WeeklyReport(PROGRAM_ID, 2);
        week2.fill("코멘트2", "영양2", "운동2", "목표2", 88.0, 4, -0.5);
        given(weeklyReportRepository.findByProgramIdOrderByWeekNumberAsc(PROGRAM_ID))
                .willReturn(List.of(week1, week2));

        mockMvc.perform(get("/programs/{programId}/weekly-reports", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weekNumber").value(1))
                .andExpect(jsonPath("$[0].avgCalorieRate").value(99.0))
                .andExpect(jsonPath("$[1].weekNumber").value(2));
    }

    @Test
    void 목록_조회_시_다른_회원_program_접근은_403() throws Exception {
        Program program = Program.create(99L, ProgramType.DIET, LocalDate.now(), LocalDate.now().plusDays(27),
                1800, 130.0, 180.0, 50.0, null);
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.of(program));

        mockMvc.perform(get("/programs/{programId}/weekly-reports", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 목록_조회_시_프로그램_미존재는_404() throws Exception {
        given(programRepository.findById(PROGRAM_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/programs/{programId}/weekly-reports", PROGRAM_ID)
                        .with(authentication(AUTH)))
                .andExpect(status().isNotFound());
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.ssafy.manager.program.presentation.WeeklyReportControllerTest"`
Expected: FAIL — `findByProgramIdOrderByWeekNumberAsc` 메서드 없음(컴파일 에러) 또는 404 핸들러 매핑 없음.

- [ ] **Step 3: Repository 메서드 추가**

`WeeklyReportRepository.java`에 import와 메서드 추가:
```java
import java.util.List;
```
인터페이스 본문에:
```java
    List<WeeklyReport> findByProgramIdOrderByWeekNumberAsc(Long programId);
```

- [ ] **Step 4: Controller 엔드포인트 추가**

`WeeklyReportController.java` import 영역에 추가:
```java
import com.ssafy.manager.program.presentation.dto.WeeklyReportResponse;
import java.util.List;
```
(이미 있으면 생략)

클래스 안 기존 `getWeeklyReport` 메서드 아래에 추가:
```java
    @GetMapping("/{programId}/weekly-reports")
    public ResponseEntity<List<WeeklyReportResponse>> getWeeklyReports(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long programId
    ) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NoSuchElementException("Program을 찾을 수 없습니다."));
        if (!program.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }
        List<WeeklyReportResponse> reports = weeklyReportRepository
                .findByProgramIdOrderByWeekNumberAsc(programId)
                .stream()
                .map(WeeklyReportResponse::from)
                .toList();
        return ResponseEntity.ok(reports);
    }
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.ssafy.manager.program.presentation.WeeklyReportControllerTest"`
Expected: PASS (기존 5개 + 신규 3개 모두 통과)

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/program/infrastructure/persistence/WeeklyReportRepository.java \
        backend/src/main/java/com/ssafy/manager/program/presentation/WeeklyReportController.java \
        backend/src/test/java/com/ssafy/manager/program/presentation/WeeklyReportControllerTest.java
git commit -m "feat(program): 주간 리포트 목록 조회 API 추가"
```

---

### Task 2: 백엔드 — 평가용 임시 dev 생성 트리거

**Files:**
- Create: `backend/src/main/java/com/ssafy/manager/global/dev/DevWeeklyReportController.java`
- Modify: `backend/src/main/java/com/ssafy/manager/global/config/SecurityConfig.java:48`
- Test: `backend/src/test/java/com/ssafy/manager/global/dev/DevWeeklyReportControllerTest.java`

**Interfaces:**
- Consumes: `WeeklyReportService.createStubs(LocalDate)` (기존 `@Transactional public void`)
- Produces: `POST /dev/weekly-reports/run?date=YYYY-MM-DD` → 200. (정식 인터페이스 아님 — 평가 후 제거)

> 이 Task의 산출물은 전부 평가용 임시물이다. 클래스 주석과 SecurityConfig 라인에 제거 표식을 남긴다.

- [ ] **Step 1: 실패하는 테스트 작성**

`DevWeeklyReportControllerTest.java` 생성:
```java
package com.ssafy.manager.global.dev;

import com.ssafy.manager.auth.infrastructure.KakaoOAuth2UserService;
import com.ssafy.manager.auth.infrastructure.KakaoOAuthSuccessHandler;
import com.ssafy.manager.global.config.JwtConfig;
import com.ssafy.manager.global.config.SecurityConfig;
import com.ssafy.manager.global.exception.GlobalExceptionHandler;
import com.ssafy.manager.program.application.WeeklyReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevWeeklyReportController.class)
@Import({SecurityConfig.class, JwtConfig.class, GlobalExceptionHandler.class})
class DevWeeklyReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WeeklyReportService weeklyReportService;
    @MockitoBean KakaoOAuth2UserService kakaoOAuth2UserService;
    @MockitoBean KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    @Test
    void 날짜를_받아_createStubs를_호출하고_인증없이_200을_반환한다() throws Exception {
        mockMvc.perform(post("/dev/weekly-reports/run").param("date", "2026-06-25"))
                .andExpect(status().isOk());

        verify(weeklyReportService).createStubs(LocalDate.of(2026, 6, 25));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.ssafy.manager.global.dev.DevWeeklyReportControllerTest"`
Expected: FAIL — `DevWeeklyReportController` 클래스 없음(컴파일 에러).

- [ ] **Step 3: dev 컨트롤러 생성**

`DevWeeklyReportController.java` 생성:
```java
package com.ssafy.manager.global.dev;

import com.ssafy.manager.program.application.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// TEMP: 주간 리포트 평가용 생성 트리거. 평가 종료 후 이 컨트롤러와
//       SecurityConfig 의 "/dev/**" permitAll 라인을 함께 제거한다.
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PostMapping("/weekly-reports/run")
    public ResponseEntity<Void> run(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        weeklyReportService.createStubs(date != null ? date : LocalDate.now());
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 4: SecurityConfig 에 /dev/** 허용 추가**

`SecurityConfig.java` 48번 라인을 다음으로 교체:
```java
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/auth/reissue", "/error").permitAll()
                        .requestMatchers("/dev/**").permitAll() // TEMP: 주간 리포트 평가용. 평가 후 제거
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.ssafy.manager.global.dev.DevWeeklyReportControllerTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ssafy/manager/global/dev/DevWeeklyReportController.java \
        backend/src/main/java/com/ssafy/manager/global/config/SecurityConfig.java \
        backend/src/test/java/com/ssafy/manager/global/dev/DevWeeklyReportControllerTest.java
git commit -m "chore(dev): 주간 리포트 평가용 임시 생성 트리거 추가 (평가 후 제거)"
```

---

### Task 3: 프론트 — 리포트 API 모듈

**Files:**
- Create: `frontend/src/api/report.js`

**Interfaces:**
- Consumes: `apiClient` (`@/services/apiClient`)
- Produces: `getWeeklyReports(programId)` → Promise<배열>, `getWeeklyReport(programId, weekNumber)` → Promise<객체>. Task 4/5가 소비.

- [ ] **Step 1: report.js 작성**

`frontend/src/api/report.js` 생성:
```js
import { apiClient } from '@/services/apiClient';

export function getWeeklyReports(programId) {
  return apiClient.get(`/programs/${programId}/weekly-reports`);
}

export function getWeeklyReport(programId, weekNumber) {
  return apiClient.get(`/programs/${programId}/weekly-reports/${weekNumber}`);
}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공(이 파일은 아직 import되지 않으므로 에러 없음).

- [ ] **Step 3: 커밋**

```bash
git add frontend/src/api/report.js
git commit -m "feat(report): 주간 리포트 조회 API 함수 추가"
```

---

### Task 4: 프론트 — 리포트 목록 화면(ReportView) + 라우트 교체

**Files:**
- Create: `frontend/src/views/ReportView.vue`
- Modify: `frontend/src/router/index.js` (import 추가, `report` 라우트 component 교체)

**Interfaces:**
- Consumes: `getWeeklyReports` (`@/api/report`), `getCurrentProgram` (`@/api/my`), vue-router `useRouter`
- Produces: 라우트 `name: 'report'` 화면. 카드 탭 시 `router.push({ name: 'report-detail', params: { weekNumber } })` (대상 라우트는 Task 5에서 등록)

- [ ] **Step 1: ReportView.vue 작성**

`frontend/src/views/ReportView.vue` 생성:
```vue
<template>
  <header class="mb-6">
    <h1 class="text-display-md text-on-background">주간 리포트</h1>
    <p class="text-label-lg text-on-surface-variant mt-1">매 7일 AI가 분석한 코칭 리포트예요.</p>
  </header>

  <div v-if="loading" class="text-on-surface-variant text-label-lg">불러오는 중…</div>

  <div v-else-if="noProgram" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background">진행 중인 프로그램이 없어요</p>
  </div>

  <div v-else-if="error" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background mb-3">리포트를 불러오지 못했어요</p>
    <button class="neo-brutal-border rounded-xl bg-nyam-mint/30 px-4 py-2" @click="load">다시 시도</button>
  </div>

  <div v-else-if="reports.length === 0" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background">아직 생성된 주간 리포트가 없어요</p>
    <p class="text-label-lg text-on-surface-variant mt-1">프로그램 시작 7일 후부터 생성돼요.</p>
  </div>

  <ul v-else class="flex flex-col gap-4">
    <li
      v-for="r in reports"
      :key="r.weekNumber"
      class="bg-white neo-brutal-border rounded-2xl p-5 cursor-pointer hover:bg-surface transition-colors"
      @click="goDetail(r.weekNumber)"
    >
      <div class="flex items-center justify-between">
        <span class="text-headline-md text-on-background font-bold">{{ r.weekNumber }}주차</span>
        <span class="text-label-lg text-on-surface-variant neo-brutal-border rounded-full px-3 py-1">
          달성 {{ r.achievementDays }}/7
        </span>
      </div>
      <div class="flex items-center justify-between mt-3">
        <span class="text-label-lg text-on-surface-variant">평균 칼로리 {{ Math.round(r.avgCalorieRate) }}%</span>
        <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
      </div>
    </li>
  </ul>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentProgram } from '@/api/my';
import { getWeeklyReports } from '@/api/report';

const router = useRouter();
const reports = ref([]);
const loading = ref(true);
const error = ref(false);
const noProgram = ref(false);

async function load() {
  loading.value = true;
  error.value = false;
  noProgram.value = false;
  try {
    const program = await getCurrentProgram();
    reports.value = await getWeeklyReports(program.programId);
  } catch (e) {
    if (e?.status === 404) {
      noProgram.value = true;
    } else {
      error.value = true;
    }
  } finally {
    loading.value = false;
  }
}

function goDetail(weekNumber) {
  router.push({ name: 'report-detail', params: { weekNumber } });
}

onMounted(load);
</script>
```

- [ ] **Step 2: 라우트 교체**

`frontend/src/router/index.js` 상단 import 영역에 추가:
```js
import ReportView from '@/views/ReportView.vue';
```
`report` 라우트(현재 `component: PlaceholderView`) 블록을 다음으로 수정:
```js
      {
        path: 'report',
        name: 'report',
        component: ReportView,
        meta: {
          title: '리포트',
          navLabel: 'Report',
        },
      },
```

- [ ] **Step 3: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공.

- [ ] **Step 4: 브라우저 수동 검증**

`npm run dev` 실행 → 카카오 로그인(member 1) → 하단 네비 `리포트` 진입.
확인: (a) 리포트가 없으면 "아직 생성된 주간 리포트가 없어요" 표시, (b) Task 2 트리거로 생성된 경우 `1주차` 카드에 `달성 5/7`, `평균 칼로리 …%` 표시.
(리포트 데이터가 아직 없으면 빈 상태만 확인하고 통과로 간주 — 카드 표시는 Task 6 E2E에서 최종 확인)

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/views/ReportView.vue frontend/src/router/index.js
git commit -m "feat(report): 주간 리포트 목록 화면 추가"
```

---

### Task 5: 프론트 — 리포트 상세 화면(ReportDetailView) + 라우트 추가

**Files:**
- Create: `frontend/src/views/ReportDetailView.vue`
- Modify: `frontend/src/router/index.js` (import 추가, `report/:weekNumber` 라우트 추가)

**Interfaces:**
- Consumes: `getWeeklyReport` (`@/api/report`), `getCurrentProgram` (`@/api/my`), vue-router `useRoute`, `apiClient` ApiError(`.status`)
- Produces: 라우트 `name: 'report-detail'`, path `report/:weekNumber`. Task 4의 `router.push`가 진입.

- [ ] **Step 1: ReportDetailView.vue 작성**

`frontend/src/views/ReportDetailView.vue` 생성:
```vue
<template>
  <header class="mb-6">
    <RouterLink
      to="/report"
      class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
    >
      <span class="material-symbols-outlined text-base">chevron_left</span>
      <span>리포트 목록으로</span>
    </RouterLink>
    <h1 class="text-display-md text-on-background">{{ weekNumber }}주차 리포트</h1>
  </header>

  <div v-if="loading" class="text-on-surface-variant text-label-lg">불러오는 중…</div>

  <div v-else-if="notFound" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background">해당 주차 리포트가 없어요</p>
  </div>

  <div v-else-if="error" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background mb-3">리포트를 불러오지 못했어요</p>
    <button class="neo-brutal-border rounded-xl bg-nyam-mint/30 px-4 py-2" @click="load">다시 시도</button>
  </div>

  <div v-else-if="report" class="flex flex-col gap-4">
    <!-- 통계 카드 -->
    <div class="bg-white neo-brutal-border rounded-2xl p-5 grid grid-cols-3 gap-2 text-center">
      <div>
        <p class="text-headline-md text-on-background font-bold">{{ Math.round(report.avgCalorieRate) }}%</p>
        <p class="text-label-md text-on-surface-variant mt-1">평균 칼로리</p>
      </div>
      <div>
        <p class="text-headline-md text-on-background font-bold">{{ report.achievementDays }}/7</p>
        <p class="text-label-md text-on-surface-variant mt-1">목표 달성일</p>
      </div>
      <div>
        <p class="text-headline-md text-on-background font-bold">{{ weightTrendText }}</p>
        <p class="text-label-md text-on-surface-variant mt-1">체중 추세</p>
      </div>
    </div>

    <!-- 종합 코멘트 -->
    <section class="bg-white neo-brutal-border rounded-2xl p-5">
      <h2 class="text-label-lg text-on-surface-variant mb-2">💬 종합 코멘트</h2>
      <p class="text-body-lg text-on-background whitespace-pre-line">{{ report.content }}</p>
    </section>

    <!-- 섹션 카드 3개 -->
    <section
      v-for="s in sections"
      :key="s.label"
      class="bg-white neo-brutal-border rounded-2xl p-5"
    >
      <h2 class="text-label-lg text-on-surface-variant mb-2">{{ s.label }}</h2>
      <p class="text-body-lg text-on-background whitespace-pre-line">{{ s.value }}</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { getCurrentProgram } from '@/api/my';
import { getWeeklyReport } from '@/api/report';

const route = useRoute();
const weekNumber = Number(route.params.weekNumber);

const report = ref(null);
const loading = ref(true);
const error = ref(false);
const notFound = ref(false);

const weightTrendText = computed(() => {
  const t = report.value?.weightTrend;
  if (t === null || t === undefined) return '기록 없음';
  const sign = t > 0 ? '+' : '';
  return `${sign}${t.toFixed(1)}kg`;
});

const sections = computed(() => report.value ? [
  { label: '🥗 영양', value: report.value.nutritionSummary },
  { label: '🏋️ 운동', value: report.value.exerciseSummary },
  { label: '🎯 목표', value: report.value.goalSummary },
] : []);

async function load() {
  loading.value = true;
  error.value = false;
  notFound.value = false;
  try {
    const program = await getCurrentProgram();
    report.value = await getWeeklyReport(program.programId, weekNumber);
  } catch (e) {
    if (e?.status === 404) {
      notFound.value = true;
    } else {
      error.value = true;
    }
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>
```

- [ ] **Step 2: 라우트 추가**

`frontend/src/router/index.js` 상단 import 영역에 추가:
```js
import ReportDetailView from '@/views/ReportDetailView.vue';
```
`report` 라우트 블록 바로 아래에 추가:
```js
      {
        path: 'report/:weekNumber',
        name: 'report-detail',
        component: ReportDetailView,
        meta: {
          title: '주간 리포트',
        },
      },
```

- [ ] **Step 3: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공.

- [ ] **Step 4: 커밋**

```bash
git add frontend/src/views/ReportDetailView.vue frontend/src/router/index.js
git commit -m "feat(report): 주간 리포트 상세 화면 추가"
```

---

### Task 6: E2E 평가 (수동)

**Files:** (코드 변경 없음 — 실행·검증만)

**Interfaces:**
- Consumes: Task 1~5 산출물 전체, 이미 삽입된 더미데이터(member 1, program start_date=2026-06-18)

- [ ] **Step 1: 서버 기동**

```bash
# FastAPI (ENV=prod → 실제 Claude 호출, 요청당 4콜)
cd ai && uvicorn app.main:app --reload
# Spring
cd backend && ./gradlew bootRun
# Vue
cd frontend && npm run dev
```
Expected: 세 서버 정상 기동.

- [ ] **Step 2: 리포트 생성 트리거**

```bash
curl -X POST "http://localhost:8080/dev/weekly-reports/run?date=2026-06-25"
```
Expected: HTTP 200. 서버 로그에 FastAPI `/ai/coaching/weekly` 호출 흔적. DB `weekly_report` 테이블에 `program_id`=(member 1의 program), `week_number`=1 row 생성.
(실패 시: createStubs는 예외를 삼키고 stub만 남길 수 있음 → 로그의 "주간 코칭 AI 호출 실패" 경고 확인, FastAPI 가동/네트워크 점검)

- [ ] **Step 3: 프론트 목록 확인**

브라우저에서 카카오 로그인(member 1) → 하단 네비 `리포트`.
Expected: `1주차` 카드에 `달성 5/7`, `평균 칼로리 …%` 표시.

- [ ] **Step 4: 프론트 상세 확인**

`1주차` 카드 탭.
Expected: 상단 통계(평균 칼로리/목표 달성일/체중 추세 약 -1.0kg) + 종합 코멘트 + 영양·운동·목표 3개 섹션이 AI 텍스트로 렌더.

- [ ] **Step 5: 결과 기록**

평가 결과(렌더 정상 여부, AI 코멘트 품질 메모)를 사용자에게 보고. 이상 발견 시 별도 태스크로 분리.

> **평가 종료 후 정리(별도 작업):** Task 2의 임시물 제거 — `DevWeeklyReportController.java` 삭제, `SecurityConfig`의 `/dev/**` permitAll 라인 삭제, 관련 테스트 삭제 후 커밋.

---

## Self-Review

- **Spec coverage:** 목록 API(§4.1)→Task1, 임시 트리거(§4.2)→Task2, report.js(§5.1)→Task3, 라우트(§5.2)→Task4·5, ReportView(§5.3)→Task4, ReportDetailView(§5.3)→Task5, 상태 처리(§5.4)→Task4·5 템플릿 분기, 레이아웃(§5.5)→Task4·5 템플릿, E2E 절차(§6)→Task6. 누락 없음.
- **Placeholder scan:** 모든 코드 스텝에 실제 코드 포함, "TBD/적절히 처리" 류 없음.
- **Type consistency:** `findByProgramIdOrderByWeekNumberAsc`(Task1 정의 = Task1 사용), `getWeeklyReports`/`getWeeklyReport`(Task3 정의 = Task4/5 사용), `program.programId`(CreateProgramResponse 필드명 일치), `report.weightTrend`/`avgCalorieRate`/`achievementDays`/`content`/`nutritionSummary`/`exerciseSummary`/`goalSummary`(WeeklyReportResponse 필드명 일치), 라우트 `report-detail`(Task4 push = Task5 등록) 일치.
