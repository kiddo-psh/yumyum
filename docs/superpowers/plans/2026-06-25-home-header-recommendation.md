# Home Header + 마지막 끼니 추천 통합 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 홈 화면 헤더의 coachMessage 영역에 마지막 끼니 추천을 통합하고, Row 2 추천 카드를 제거해 퀵액션을 full-width 3칸으로 재배치한다.

**Architecture:** `HomeView.vue` 단일 파일만 수정. 헤더의 `<p>{{ coachMessage }}</p>` 자리를 3-state 분기(기본 멘트 / 로딩 / 추천 준비)로 교체하고, Row 2에서 추천 카드를 제거한 뒤 퀵액션을 `grid-cols-3`으로 확장한다. JS 로직(`loadRecommendation`, `hasRecommendation`, `recommendation`)은 그대로 유지.

**Tech Stack:** Vue 3, Tailwind CSS, Vue Router (RouterLink)

## Global Constraints

- `neo-brutal-border`, `neo-brutal-card-hover` 클래스 패턴 유지
- `ResponseEntity` / Spring 무관 — 프론트 전용 변경
- `RouterLink` name: `meal-manual`, query: `{ q: 음식명 }` 경로 유지

---

### Task 1: 헤더 coachMessage 영역 3-state 분기로 교체

**Files:**
- Modify: `frontend/src/views/HomeView.vue` (헤더 template 영역, coachMessage computed)

**Interfaces:**
- Consumes: `hasRecommendation` (computed, Boolean), `recommendation.name`, `recommendation.reason` (computed), `state.balance?.lastMealRecommendTrigger` (Boolean), `state.recommendLoading` (Boolean), `coachMessage` (computed, String)
- Produces: 헤더에 3가지 상태 UI

- [ ] **Step 1: 헤더 template 교체**

`HomeView.vue` 의 아래 블록을:

```html
<div>
  <h2 class="text-headline-lg text-on-background">안녕! 오늘 하루는 어때요?</h2>
  <p class="text-body-md text-on-surface-variant">
    {{ coachMessage }}
  </p>
</div>
```

다음으로 교체:

```html
<div>
  <h2 class="text-headline-lg text-on-background">안녕! 오늘 하루는 어때요?</h2>
  <!-- 추천 준비됨 -->
  <div v-if="hasRecommendation" class="flex items-center gap-3 mt-1 flex-wrap">
    <p class="text-body-md text-on-surface-variant">
      <span class="font-bold text-on-background">{{ recommendation.name }}</span> 어때요?
      {{ recommendation.reason }}
    </p>
    <RouterLink
      :to="{ name: 'meal-manual', query: { q: recommendation.name } }"
      class="shrink-0 bg-primary text-white px-4 py-1.5 neo-brutal-border rounded-lg text-label-sm font-bold hover:-translate-y-0.5 transition-transform"
    >
      식단에 추가
    </RouterLink>
  </div>
  <!-- 추천 로딩 중 -->
  <p
    v-else-if="state.balance?.lastMealRecommendTrigger && state.recommendLoading"
    class="text-body-md text-on-surface-variant"
  >
    마지막 끼니 추천을 가져오는 중...
  </p>
  <!-- 기본 AI 멘트 -->
  <p v-else class="text-body-md text-on-surface-variant">
    {{ coachMessage }}
  </p>
</div>
```

- [ ] **Step 2: coachMessage computed에서 trigger 분기 제거**

기존:
```js
const coachMessage = computed(() => {
  if (!isProgramReady.value) return 'AI가 맞춤 플랜을 생성하고 있어요...'
  if (aiComment.value) return aiComment.value
  if (state.balance?.lastMealRecommendTrigger) return '마지막 끼니 추천이 준비됐어요!'
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 기록해볼까요?`
})
```

변경 후:
```js
const coachMessage = computed(() => {
  if (!isProgramReady.value) return 'AI가 맞춤 플랜을 생성하고 있어요...'
  if (aiComment.value) return aiComment.value
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 기록해볼까요?`
})
```

- [ ] **Step 3: 브라우저에서 3가지 상태 확인**

  - `lastMealRecommendTrigger: false` → 기본 AI 멘트 또는 남은 칼로리 표시
  - 개발자 도구 → `state.recommendLoading = true` 강제 → "가져오는 중..." 텍스트 확인
  - `state.recommendData`에 더미 데이터 주입 → 음식명 + 이유 + 버튼 표시 확인

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/HomeView.vue
git commit -m "feat(home): 헤더에 마지막 끼니 추천 인라인 통합"
```

---

### Task 2: Row 2 추천 카드 제거 + 퀵액션 full-width 재배치

**Files:**
- Modify: `frontend/src/views/HomeView.vue` (Row 2 template 영역)

**Interfaces:**
- Consumes: `weightSuccess`, `weightError`, `weightSubmitting`, `newWeight`, `submitWeight()` (기존 그대로)
- Produces: Row 2가 `grid-cols-3` full-width 퀵액션으로 교체됨

- [ ] **Step 1: Row 2 전체 블록 교체**

기존 Row 2 전체 (`<!-- Row 2: Recommendation & Quick Actions -->` 주석부터 닫는 `</div>`까지)를 아래로 교체:

```html
<!-- Row 2: Quick Actions -->
<div v-if="isProgramReady" class="grid grid-cols-3 gap-4">
  <RouterLink
    to="/meals/search"
    class="bg-white neo-brutal-border rounded-xl p-6 flex flex-col items-center justify-center text-center neo-brutal-card-hover"
  >
    <span class="material-symbols-outlined text-4xl mb-2 text-primary">edit_note</span>
    <span class="font-bold">식단 기록</span>
  </RouterLink>
  <RouterLink
    to="/meals/photo"
    class="bg-white neo-brutal-border rounded-xl p-6 flex flex-col items-center justify-center text-center neo-brutal-card-hover"
  >
    <span class="material-symbols-outlined text-4xl mb-2 text-primary">camera_enhance</span>
    <span class="font-bold">사진 분석</span>
  </RouterLink>
  <div class="bg-white neo-brutal-border rounded-xl p-5 neo-brutal-card-hover">
    <div class="flex items-center gap-2 mb-3">
      <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">monitor_weight</span>
      <span class="font-bold text-sm">오늘 체중</span>
      <span v-if="weightSuccess" class="ml-auto text-success text-xs font-bold flex items-center gap-1">
        <span class="material-symbols-outlined text-sm" style="font-variation-settings:'FILL' 1;">check_circle</span>기록 완료
      </span>
    </div>
    <p v-if="weightError" class="text-danger text-xs font-bold mb-2">{{ weightError }}</p>
    <form class="flex gap-2" @submit.prevent="submitWeight">
      <input
        v-model.number="newWeight"
        type="number"
        step="0.1"
        placeholder="kg 입력"
        class="flex-1 neo-brutal-border rounded-lg px-3 py-2 text-sm bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
        required
      />
      <button
        type="submit"
        class="px-4 py-2 bg-primary text-white neo-brutal-border rounded-lg text-sm font-bold disabled:opacity-50"
        :disabled="weightSubmitting"
      >
        {{ weightSubmitting ? '...' : '기록' }}
      </button>
    </form>
  </div>
</div>
```

- [ ] **Step 2: 브라우저에서 Row 2 확인**

  - 퀵액션 3칸(식단 기록 / 사진 분석 / 오늘 체중)이 가로로 나란히 표시되는지 확인
  - 추천 카드가 사라졌는지 확인
  - 체중 기록 폼 정상 동작 확인

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/HomeView.vue
git commit -m "refactor(home): 추천 카드 제거 및 퀵액션 3칸 full-width 재배치"
```
