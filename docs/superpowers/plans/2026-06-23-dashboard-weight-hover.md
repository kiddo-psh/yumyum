# Dashboard 체중 입력 이동 + 카드 호버 이펙트 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 대시보드 체중 추세 카드에 체중 입력 폼을 추가하고, 마이페이지 WeightSection을 제거하며, 두 탭의 카드 전체에 호버 리프트 이펙트를 통일한다.

**Architecture:** Vue 3 SFC 수정만 포함한다. 백엔드 변경 없음. 체중 데이터는 `POST /weights` → 성공 시 `state.weights`에 정규화 삽입(낙관적 업데이트, 재요청 없음). DashboardView의 필드명 버그(`w.date`/`w.weight`)를 `loadWeights` 정규화로 수정한다.

**Tech Stack:** Vue 3 `<script setup>`, Tailwind CSS, `apiClient` (`@/services/apiClient`)

## Global Constraints

- Vue 3 `<script setup>` 문법. `import { ref, computed, ... } from 'vue'` 명시적 임포트 필수.
- `neo-brutal-card-hover` 클래스는 `frontend/src/styles/tailwind.css`에 이미 정의됨 (hover 시 `translate(-4px,-4px)` + shadow 확대). 별도 스타일 추가 금지.
- `apiClient`는 `@/services/apiClient`에서 임포트.
- 자동화 테스트 없음. 검증은 `http://localhost:5173` 브라우저 확인으로 대체.
- 커밋 메시지 형식: `feat(scope): 한글 설명`

---

### Task 1: Dashboard — 체중 입력 폼 + 필드명 버그 수정 + 호버 이펙트

**Files:**
- Modify: `frontend/src/views/DashboardView.vue`

**Interfaces:**
- Consumes: `apiClient.post('/weights', { weightKg, recordedDate })` → `{ id, weightKg, recordedDate }`
- Produces: 없음 (Task 2와 독립)

**버그 컨텍스트:** `getWeightHistory()`는 `{ id, weightKg, recordedDate }` 배열을 반환하지만, 기존 차트 코드는 `w.date`, `w.weight`로 접근 → 차트 데이터가 항상 빈 배열. `loadWeights`에서 정규화하면 기존 차트 코드를 모두 건드리지 않고 수정 가능.

- [ ] **Step 1: `loadWeights`에 정규화 추가**

`frontend/src/views/DashboardView.vue`의 `loadWeights` 함수를 찾아 아래로 교체:

```js
async function loadWeights() {
  state.weightsLoading = true
  try {
    const raw = await getWeightHistory() ?? []
    state.weights = raw.map(w => ({ id: w.id, date: w.recordedDate, weight: w.weightKg }))
  } catch {
    state.weights = []
  } finally {
    state.weightsLoading = false
  }
}
```

- [ ] **Step 2: `apiClient` 임포트 및 체중 입력 상태 추가**

`<script setup>` 맨 위 임포트 목록에 추가:
```js
import { apiClient } from '@/services/apiClient'
```

`viewMode = ref('daily')` 바로 아래에 추가:
```js
const newWeight = ref(null)
const newDate = ref(today)
const submitting = ref(false)
const weightSubmitError = ref('')
```

- [ ] **Step 3: `submitWeight` 함수 추가**

`loadWeights` 함수 바로 아래에 추가:
```js
async function submitWeight() {
  submitting.value = true
  weightSubmitError.value = ''
  try {
    const created = await apiClient.post('/weights', {
      weightKg: newWeight.value,
      recordedDate: newDate.value,
    })
    state.weights = [...state.weights, { id: created.id, date: created.recordedDate, weight: created.weightKg }]
      .sort((a, b) => a.date.localeCompare(b.date))
    newWeight.value = null
  } catch {
    weightSubmitError.value = '체중 기록에 실패했어요.'
  } finally {
    submitting.value = false
  }
}
```

- [ ] **Step 4: 체중 추세 카드에 입력 폼 추가 + `neo-brutal-card-hover` 적용**

`<!-- 체중 추세 그래프 -->` div (`col-span-7 ...`)를 아래로 교체:

```html
<!-- 체중 추세 그래프 -->
<div class="col-span-7 bg-surface neo-brutal-border rounded-xl p-8 neo-brutal-card-hover">
  <!-- 헤더 -->
  <div class="flex items-center justify-between mb-6">
    <h2 class="text-headline-lg text-on-background">체중 추세</h2>
    <div class="flex items-center gap-3">
      <span v-if="latestWeight" class="text-headline-md font-black text-primary">
        {{ latestWeight }} kg
      </span>
      <!-- 일/주 토글 -->
      <div class="flex neo-brutal-border rounded-lg overflow-hidden">
        <button
          class="px-3 py-1.5 text-xs font-black transition-colors"
          :class="viewMode === 'daily' ? 'bg-on-background text-white' : 'bg-white hover:bg-surface'"
          @click="viewMode = 'daily'"
        >일</button>
        <button
          class="px-3 py-1.5 text-xs font-black border-l-[3px] border-on-background transition-colors"
          :class="viewMode === 'weekly' ? 'bg-on-background text-white' : 'bg-white hover:bg-surface'"
          @click="viewMode = 'weekly'"
        >주</button>
      </div>
    </div>
  </div>

  <div v-if="state.weightsLoading" class="flex items-center gap-3 text-on-surface-variant py-10">
    <span class="material-symbols-outlined animate-spin">progress_activity</span>
    <span>불러오는 중...</span>
  </div>

  <div v-else-if="!weightPoints.length" class="py-12 text-center text-on-surface-variant">
    <span class="material-symbols-outlined text-5xl block mb-3 opacity-30" style="font-variation-settings:'FILL' 1;">monitor_weight</span>
    <p class="text-body-md">아직 체중 기록이 없어요.</p>
    <p class="text-body-sm opacity-60 mt-1">체중을 기록하면 추세 그래프가 표시됩니다.</p>
  </div>

  <template v-else>
    <svg :viewBox="`0 0 ${CHART_W} ${CHART_H}`" class="w-full overflow-visible" style="height: 200px;">
      <defs>
        <linearGradient id="weight-fill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#FF8C42" stop-opacity="0.18"/>
          <stop offset="100%" stop-color="#FF8C42" stop-opacity="0"/>
        </linearGradient>
      </defs>
      <path :d="fillPath(weightPoints)" fill="url(#weight-fill)" />
      <path
        :d="smoothPath(weightPoints)"
        fill="none"
        stroke="#FF8C42"
        stroke-width="3.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <circle
        v-for="(p, i) in weightPoints"
        :key="i"
        :cx="p.x"
        :cy="p.y"
        r="4.5"
        fill="#FF8C42"
        stroke="white"
        stroke-width="2.5"
      />
      <text
        v-for="(p, i) in xLabels"
        :key="`xl-${i}`"
        :x="p.x"
        :y="CHART_H - 4"
        text-anchor="middle"
        font-size="11"
        font-weight="700"
        fill="#AAAAAA"
      >{{ p.label }}</text>
    </svg>
    <div class="flex justify-between text-xs font-bold mt-3 px-1">
      <span class="text-on-surface-variant">최저 <span class="text-on-background">{{ weightMin }}kg</span></span>
      <span class="text-on-surface-variant">최고 <span class="text-on-background">{{ weightMax }}kg</span></span>
      <span class="text-on-surface-variant">평균 <span class="text-on-background">{{ weightAvg }}kg</span></span>
    </div>
  </template>

  <!-- 체중 입력 폼 -->
  <div class="border-t-[3px] border-on-background mt-6 pt-5">
    <p v-if="weightSubmitError" class="text-danger text-xs font-bold mb-2">{{ weightSubmitError }}</p>
    <form class="flex gap-3" @submit.prevent="submitWeight">
      <input
        v-model.number="newWeight"
        type="number"
        step="0.1"
        placeholder="오늘 체중 (kg)"
        class="flex-1 neo-brutal-border rounded-lg px-3 py-2 text-body-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary"
        required
      />
      <input
        v-model="newDate"
        type="date"
        class="neo-brutal-border rounded-lg px-3 py-2 text-body-sm bg-white"
        required
      />
      <button
        type="submit"
        class="px-4 py-2 bg-primary text-white neo-brutal-border rounded-lg text-label-lg disabled:opacity-50"
        :disabled="submitting"
      >
        {{ submitting ? '저장 중' : '기록' }}
      </button>
    </form>
  </div>
</div>
```

- [ ] **Step 5: 주간 달성 현황 카드에 `neo-brutal-card-hover` 추가**

`<!-- 주간 달성 현황 -->` div의 클래스:
```
class="col-span-8 bg-surface neo-brutal-border rounded-xl p-6"
```
→
```
class="col-span-8 bg-surface neo-brutal-border rounded-xl p-6 neo-brutal-card-hover"
```

- [ ] **Step 6: AI 채팅 카드에 `neo-brutal-card-hover` 추가**

`<!-- AI 채팅 -->` div의 클래스:
```
class="col-span-5 bg-surface neo-brutal-border rounded-xl flex flex-col overflow-hidden"
```
→
```
class="col-span-5 bg-surface neo-brutal-border rounded-xl flex flex-col overflow-hidden neo-brutal-card-hover"
```

- [ ] **Step 7: 브라우저 확인**

`http://localhost:5173` 대시보드 탭에서:
- 주간 달성 현황 카드: hover 시 카드가 위로 올라가고 그림자 확대
- 달성률 카드: 이미 있으므로 동일 동작 확인
- 체중 추세 카드: hover 이펙트 + 하단에 kg/날짜/기록 버튼 폼 표시
- 체중 기록 제출 후 차트에 바로 반영되는지 확인
- AI 채팅 카드: hover 이펙트 확인

- [ ] **Step 8: 커밋**

```bash
git add frontend/src/views/DashboardView.vue
git commit -m "feat(dashboard): 체중 입력 폼 + 필드명 버그 수정 + 카드 호버 이펙트"
```

---

### Task 2: MyPage — WeightSection 제거 + 카드 호버 이펙트

**Files:**
- Modify: `frontend/src/views/MyView.vue`
- Modify: `frontend/src/components/my/NyamSection.vue`
- Modify: `frontend/src/components/my/ProfileSection.vue`
- Modify: `frontend/src/components/my/ProgramSection.vue`
- Delete: `frontend/src/components/my/WeightSection.vue`

**Interfaces:**
- Consumes: Task 1과 독립. Task 1 완료 여부와 무관하게 실행 가능.

- [ ] **Step 1: MyView.vue에서 WeightSection 제거**

`frontend/src/views/MyView.vue`의 `<script setup>`에서:
- `import WeightSection from '@/components/my/WeightSection.vue'` 라인 삭제
- `import { getWeightHistory } from '@/api/dashboard'` 라인 삭제
- `const weights = ref([])` 라인 삭제
- `const weightsError = ref(null)` 라인 삭제
- `onMounted` 안의 `getWeightHistory()` 관련 코드 삭제:
  ```js
  // 삭제할 라인들:
  const profileResult = await Promise.allSettled([getMyProfile(), getNyamStatus(), getWeightHistory()])
  // → 아래로 교체:
  const profileResult = await Promise.allSettled([getMyProfile(), getNyamStatus()])
  ```
  그리고 `profileResult[2]` 처리 블록 전체 삭제:
  ```js
  // 삭제:
  if (profileResult[2].status === 'fulfilled') {
    weights.value = profileResult[2].value ?? [];
  } else {
    weightsError.value = profileResult[2].reason;
  }
  ```

`<template>`에서:
- `<WeightSection :initial-weights="weights" :error="weightsError" />` 라인 삭제

최종 `MyView.vue` 전체 모습:

```vue
<template>
  <header class="flex justify-between items-center mb-10">
    <div>
      <h1 class="text-display-md text-on-background">마이페이지</h1>
    </div>
  </header>

  <div class="flex flex-col gap-8">
    <NyamSection :nyam="nyam" :error="nyamError" />
    <ProfileSection :profile="profile" :error="profileError" @updated="onProfileUpdated" />
    <ProgramSection :program="program" :error="programError" />

    <!-- 로그아웃 -->
    <div class="flex justify-end">
      <button class="px-6 py-3 neo-brutal-border rounded-xl text-label-lg text-danger hover:bg-surface transition-colors"
              @click="logout">
        로그아웃
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { apiClient } from '@/services/apiClient';
import { getMyProfile, getNyamStatus, getCurrentProgram } from '@/api/my';
import NyamSection from '@/components/my/NyamSection.vue';
import ProfileSection from '@/components/my/ProfileSection.vue';
import ProgramSection from '@/components/my/ProgramSection.vue';

const router = useRouter();

const nyam = ref(null);
const profile = ref(null);
const program = ref(null);

const nyamError = ref(null);
const profileError = ref(null);
const programError = ref(null);

onMounted(async () => {
  const profileResult = await Promise.allSettled([getMyProfile(), getNyamStatus()]);

  if (profileResult[0].status === 'fulfilled') {
    profile.value = profileResult[0].value;
  } else {
    profileError.value = profileResult[0].reason;
  }

  if (profileResult[1].status === 'fulfilled') {
    nyam.value = profileResult[1].value;
  } else {
    nyamError.value = profileResult[1].reason;
  }

  if (profile.value?.memberId) {
    const programResult = await Promise.allSettled([getCurrentProgram(profile.value.memberId)]);
    if (programResult[0].status === 'fulfilled') {
      program.value = programResult[0].value;
    } else {
      programError.value = programResult[0].reason;
    }
  } else {
    programError.value = new Error('프로필 로딩 실패로 Program을 조회할 수 없습니다.');
  }
});

function onProfileUpdated(updated) {
  profile.value = updated;
}

async function logout() {
  await apiClient.post('/auth/logout');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  router.push('/');
}
</script>
```

- [ ] **Step 2: NyamSection.vue에 `neo-brutal-card-hover` 추가**

`frontend/src/components/my/NyamSection.vue`의 `<section>` 태그:
```html
<section class="neo-brutal-border rounded-xl p-8 flex items-center gap-8"
```
→
```html
<section class="neo-brutal-border rounded-xl p-8 flex items-center gap-8 neo-brutal-card-hover"
```

- [ ] **Step 3: ProfileSection.vue에 `neo-brutal-card-hover` 추가**

`frontend/src/components/my/ProfileSection.vue`의 `<section>` 태그:
```html
<section class="neo-brutal-border rounded-xl bg-surface p-8">
```
→
```html
<section class="neo-brutal-border rounded-xl bg-surface p-8 neo-brutal-card-hover">
```

- [ ] **Step 4: ProgramSection.vue에 `neo-brutal-card-hover` 추가**

`frontend/src/components/my/ProgramSection.vue`의 `<section>` 태그:
```html
<section class="neo-brutal-border rounded-xl bg-surface p-8">
```
→
```html
<section class="neo-brutal-border rounded-xl bg-surface p-8 neo-brutal-card-hover">
```

- [ ] **Step 5: WeightSection.vue 파일 삭제**

```bash
git rm frontend/src/components/my/WeightSection.vue
```

- [ ] **Step 6: 브라우저 확인**

`http://localhost:5173/my` 마이페이지에서:
- 체중 기록 섹션이 사라졌는지 확인
- Nyam 카드, 프로필 카드, Program 카드 hover 시 리프트 이펙트 확인
- 로그아웃 버튼 정상 표시 확인

- [ ] **Step 7: 커밋**

```bash
git add frontend/src/views/MyView.vue \
        frontend/src/components/my/NyamSection.vue \
        frontend/src/components/my/ProfileSection.vue \
        frontend/src/components/my/ProgramSection.vue
git commit -m "feat(my): WeightSection 제거 + 카드 호버 이펙트 추가"
```
