<template>
  <!-- TopAppBar -->
  <header class="flex justify-between items-center mb-10 w-full">
    <div class="flex items-center gap-4">
      <div class="relative w-16 h-16 bg-nyam-mint rounded-xl neo-brutal-border flex items-center justify-center overflow-visible flex-shrink-0">
        <img src="/nyam/nyamnyam.png" alt="냠냠이" class="w-12 h-12 object-contain" />
        <div v-if="currentStreak > 0" class="absolute -top-2 -right-2 bg-warning neo-brutal-border px-2 py-1 rounded-full text-[10px] font-bold text-on-background">
          🔥 {{ currentStreak }}일
        </div>
        <div v-else class="absolute -top-2 -right-2 bg-warning neo-brutal-border px-2 py-1 rounded-full text-[10px] font-bold text-on-background">
          New!
        </div>
      </div>
      <div>
        <h2 class="text-headline-lg text-on-background">안녕! 오늘 하루는 어때요?</h2>
        <p class="text-body-md text-on-surface-variant">
          {{ coachMessage }}
        </p>
      </div>
    </div>
    <div class="flex gap-4">
      <button class="w-12 h-12 flex items-center justify-center neo-brutal-border rounded-full hover:bg-surface transition-colors">
        <span class="material-symbols-outlined">notifications</span>
      </button>
      <RouterLink to="/my" class="w-12 h-12 flex items-center justify-center neo-brutal-border rounded-full hover:bg-surface transition-colors">
        <span class="material-symbols-outlined">settings</span>
      </RouterLink>
    </div>
  </header>

  <!-- 프로그램 생성 대기 중 로딩 카드 -->
  <div v-if="!state.loading && !isProgramReady" class="mb-8">
    <div class="bg-surface neo-brutal-border rounded-xl p-12 flex flex-col items-center justify-center gap-6 text-center" style="min-height: 320px;">
      <div class="plan-spinner" />
      <div>
        <p class="text-headline-lg text-on-background mb-2">맞춤 플랜을 생성하고 있어요</p>
        <p class="text-body-md text-on-surface-variant">BMR·TDEE를 계산해 목표 칼로리와 영양소를 설정 중이에요.<br/>잠시만 기다려 주세요!</p>
      </div>
      <div class="flex gap-3">
        <span v-for="i in 3" :key="i" class="plan-dot" :style="{ animationDelay: (i - 1) * 0.25 + 's' }" />
      </div>
    </div>
  </div>

  <!-- Row 1: Achievement & Calories -->
  <div v-if="isProgramReady" class="grid grid-cols-12 gap-8 mb-8">
    <!-- Achievement Card -->
    <div class="col-span-7 bg-surface neo-brutal-border rounded-xl p-8 relative overflow-hidden neo-brutal-card-hover">
      <div class="flex items-center justify-between gap-8 h-full">
        <div class="flex-1">
          <div class="inline-block px-4 py-2 bg-white border-[3px] border-on-background rounded-full text-label-lg mb-4">
            목표 달성도
          </div>
          <h3 class="text-display-md mb-2">{{ achievementMessage }}</h3>
          <p class="text-body-lg text-on-surface-variant mb-6">
            목표까지 단 {{ 100 - calorieProgress }}% 남았네요. <br />조금만 더 힘내볼까요?
          </p>
          <div class="relative w-48 h-48 bg-nyam-mint neo-brutal-border rounded-2xl p-4 flex items-center justify-center">
            <img src="/nyam/nyamnyam.png" alt="냠냠이" class="w-full h-full object-contain" />
          </div>
        </div>

        <!-- Progress Circle -->
        <div class="relative w-56 h-56 flex-shrink-0 flex items-center justify-center">
          <svg class="w-full h-full progress-circle-svg absolute" viewBox="0 0 100 100">
            <circle class="text-outline" cx="50" cy="50" r="42" fill="transparent" stroke="currentColor" stroke-width="6" />
            <circle
              ref="progressCircle"
              class="text-success"
              cx="50" cy="50" r="42"
              fill="transparent"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-width="12"
              :stroke-dasharray="circumference"
              :stroke-dashoffset="strokeDashoffset"
              style="transition: stroke-dashoffset 1.5s cubic-bezier(0.34, 1.56, 0.64, 1);"
            />
          </svg>
          <div class="flex flex-col items-center justify-center z-10">
            <span class="text-display-md leading-none">{{ calorieProgress }}</span>
            <span class="text-label-lg text-on-surface-variant">%</span>
          </div>
          <div class="absolute -bottom-2 bg-on-background text-white px-4 py-1 rounded-full text-sm font-black">
            Progress
          </div>
        </div>
      </div>
    </div>

    <!-- Today's Calories Card -->
    <div class="col-span-5 bg-surface neo-brutal-border rounded-xl px-8 pt-14 pb-8 neo-brutal-card-hover">
      <div class="flex justify-between items-start mb-8">
        <div>
          <p class="text-label-lg text-on-surface-variant uppercase tracking-widest">오늘의 칼로리</p>
          <h3 class="text-numeral-xl mt-2">
            {{ formatNumber(intakeCalories) }}
            <span class="text-headline-md text-on-surface-variant">kcal</span>
          </h3>
        </div>
        <div class="w-16 h-16 bg-primary neo-brutal-border rounded-2xl flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-white text-3xl" style="font-variation-settings:'FILL' 1;">restaurant</span>
        </div>
      </div>

      <div class="space-y-6">
        <!-- Calorie Progress Bar -->
        <div>
          <div class="flex justify-between mb-2">
            <span class="font-bold">남은 칼로리</span>
            <span class="font-bold text-primary">{{ formatNumber(remainingCalories) }} kcal</span>
          </div>
          <div class="w-full h-8 bg-white neo-brutal-border rounded-full overflow-hidden relative">
            <div
              class="h-full bg-primary border-r-[3px] border-on-background transition-all duration-700"
              :style="{ width: calorieProgress + '%' }"
            />
            <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
              <span class="text-[10px] font-black uppercase text-on-background">
                Goal: {{ formatNumber(targetCalories) }} kcal
              </span>
            </div>
          </div>
        </div>

        <!-- Macro Bars -->
        <div class="grid grid-cols-3 gap-4">
          <div v-for="macro in macros" :key="macro.key" class="text-center">
            <p class="text-xs font-bold text-on-surface-variant mb-1">{{ macro.label }}</p>
            <div class="h-16 w-full bg-white neo-brutal-border rounded-lg relative flex flex-col justify-end overflow-hidden">
              <div
                class="w-full border-t-[3px] border-on-background transition-all duration-700"
                :class="macro.colorClass"
                :style="{ height: macro.percent + '%' }"
              />
              <span class="absolute inset-0 flex items-center justify-center text-xs font-bold">
                {{ formatNumber(macro.current) }}g
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Row 2: Recommendation & Quick Actions -->
  <div v-if="isProgramReady" class="grid grid-cols-12 gap-8">
    <!-- Meal Recommendation Card -->
    <div class="col-span-8 bg-surface neo-brutal-border rounded-xl p-8 neo-brutal-card-hover">
      <div v-if="state.loading" class="flex items-center gap-4 text-on-surface-variant">
        <span class="material-symbols-outlined animate-spin">progress_activity</span>
        <span class="text-body-md">AI 추천을 불러오는 중...</span>
      </div>
      <div v-else class="flex items-center gap-8">
        <div class="w-40 h-40 neo-brutal-border rounded-2xl overflow-hidden flex-shrink-0 bg-white flex items-center justify-center">
          <span class="material-symbols-outlined text-[64px] text-on-surface-variant" style="font-variation-settings:'FILL' 1;">restaurant_menu</span>
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-3">
            <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">recommend</span>
            <span class="font-bold text-on-surface-variant">마지막 끼니 추천</span>
          </div>
          <h4 class="text-headline-lg mb-2">{{ recommendation.name }}</h4>
          <p class="text-body-md mb-6 text-on-surface-variant">{{ recommendation.reason }}</p>
          <div class="flex gap-4">
            <RouterLink
              v-if="hasRecommendation"
              :to="{ name: 'meal-manual', query: { q: recommendation.name } }"
              class="bg-primary text-white px-8 py-3 neo-brutal-border rounded-lg font-bold flex items-center gap-2 hover:-translate-y-1 transition-transform"
            >
              <span class="material-symbols-outlined">add_circle</span>
              식단에 추가
            </RouterLink>
            <button
              class="bg-white px-8 py-3 neo-brutal-border rounded-lg font-bold flex items-center gap-2 hover:-translate-y-1 transition-transform"
              :disabled="state.recommendLoading"
              @click="loadRecommendation"
            >
              <span class="material-symbols-outlined">refresh</span>
              {{ state.recommendLoading ? '요청 중...' : (hasRecommendation ? '다시 추천' : '추천 받기') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Action Bento -->
    <div class="col-span-4 grid grid-cols-2 gap-4">
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
      <!-- 체중 입력 -->
      <div class="col-span-2 bg-white neo-brutal-border rounded-xl p-5 neo-brutal-card-hover">
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getCalorieBalance,
  getDailySummary,
  getHomeComment,
  getLastMealRecommendation,
} from '@/api/dashboard'
import { apiClient } from '@/services/apiClient'
import { getEffectiveToday } from '@/utils/effectiveDate'

const RADIUS = 42
const circumference = 2 * Math.PI * RADIUS

const state = reactive({
  loading: true,
  summary: null,
  balance: null,
  recommendLoading: false,
  recommendData: null,
})

const progressCircle = ref(null)
const strokeDashoffset = ref(circumference)

const aiComment = ref(null)

const newWeight = ref(null)
const weightSubmitting = ref(false)
const weightError = ref('')
const weightSuccess = ref(false)

const today = formatDate(getEffectiveToday())

const targetCalories = computed(() => state.balance?.targetCalories ?? state.summary?.targetCalories ?? 1800)
const intakeCalories = computed(() => state.balance?.intakeCalories ?? state.summary?.achievedCalories ?? 0)
const remainingCalories = computed(() => Math.max(targetCalories.value - intakeCalories.value, 0))
const currentStreak = computed(() => state.summary?.currentStreak ?? 0)

const calorieProgress = computed(() =>
  clamp(targetCalories.value ? Math.round((intakeCalories.value / targetCalories.value) * 100) : 0)
)

const achievementMessage = computed(() => {
  const p = calorieProgress.value
  if (p >= 100) return '완벽해요! 🎉'
  if (p >= 80) return '거의 다 왔어요!'
  if (p >= 50) return '잘 하고 있어요!'
  return '오늘도 화이팅!'
})

const isProgramReady = computed(() => (state.balance?.targetCalories ?? 0) > 0)

const coachMessage = computed(() => {
  if (!isProgramReady.value) return 'AI가 맞춤 플랜을 생성하고 있어요...'
  if (aiComment.value) return aiComment.value
  if (state.balance?.lastMealRecommendTrigger) return '마지막 끼니 추천이 준비됐어요!'
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 기록해볼까요?`
})

const macros = computed(() => [
  {
    key: 'carbs',
    label: '탄수화물',
    current: state.summary?.totalCarbs ?? 0,
    target: 250,
    percent: clamp(Math.round(((state.summary?.totalCarbs ?? 0) / 250) * 100)),
    colorClass: 'bg-carbs',
  },
  {
    key: 'protein',
    label: '단백질',
    current: state.summary?.totalProtein ?? 0,
    target: 120,
    percent: clamp(Math.round(((state.summary?.totalProtein ?? 0) / 120) * 100)),
    colorClass: 'bg-protein',
  },
  {
    key: 'fat',
    label: '지방',
    current: state.summary?.totalFat ?? 0,
    target: 60,
    percent: clamp(Math.round(((state.summary?.totalFat ?? 0) / 60) * 100)),
    colorClass: 'bg-fat',
  },
])

const hasRecommendation = computed(() => Boolean(state.recommendData?.recommendations?.[0]))

const recommendation = computed(() => {
  const rec = state.recommendData?.recommendations?.[0]
  if (rec) return { name: rec.name, reason: rec.reason ?? '잔여 영양소 기반 AI 추천' }
  return {
    name: remainingCalories.value > 0 ? '추천 받기' : '오늘 목표 달성!',
    reason: state.balance?.lastMealRecommendTrigger
      ? '아래 버튼으로 AI 추천을 받아보세요.'
      : '조건이 충족되면 자동으로 추천됩니다.',
  }
})

onMounted(async () => {
  const [summary, balance] = await Promise.allSettled([
    getDailySummary(today),
    getCalorieBalance(today),
  ])

  state.summary = summary.status === 'fulfilled' ? summary.value : null
  state.balance = balance.status === 'fulfilled' ? balance.value : null
  state.loading = false

  // animate progress circle
  setTimeout(() => {
    strokeDashoffset.value = circumference - (calorieProgress.value / 100) * circumference
  }, 300)

  if (state.balance?.lastMealRecommendTrigger) {
    loadRecommendation()
  }

  // 온보딩 직후: 목표칼로리가 0이면 Program 비동기 생성 완료를 기다려 자동 갱신
  if (!state.balance?.targetCalories) {
    pollForProgram()
  }

  // AI 코멘트 비동기 로드 (실패 시 정적 fallback 유지)
  try {
    const res = await getHomeComment()
    aiComment.value = res.comment
  } catch {
    // aiComment null 유지 → coachMessage computed가 정적 fallback 반환
  }
})

async function pollForProgram(attempts = 0) {
  if (attempts >= 6) return   // 최대 12초(2s×6) 시도 후 포기
  await new Promise(r => setTimeout(r, 2000))
  try {
    const balance = await getCalorieBalance(today)
    if (balance?.targetCalories > 0) {
      state.balance = balance
      setTimeout(() => {
        strokeDashoffset.value = circumference - (calorieProgress.value / 100) * circumference
      }, 100)
      return
    }
  } catch { /* 무시 */ }
  pollForProgram(attempts + 1)
}

async function loadRecommendation() {
  state.recommendLoading = true
  try {
    state.recommendData = await getLastMealRecommendation()
  } catch {
    // recommendation not available yet
  } finally {
    state.recommendLoading = false
  }
}

async function submitWeight() {
  weightSubmitting.value = true
  weightError.value = ''
  weightSuccess.value = false
  try {
    await apiClient.post('/weights', { weightKg: newWeight.value, recordedDate: today })
    newWeight.value = null
    weightSuccess.value = true
    setTimeout(() => { weightSuccess.value = false }, 3000)
  } catch {
    weightError.value = '체중 기록에 실패했어요.'
  } finally {
    weightSubmitting.value = false
  }
}

function clamp(v) {
  return Math.max(0, Math.min(100, v))
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatNumber(v) {
  return Math.round(Number(v) || 0).toLocaleString('ko-KR')
}
</script>

<style scoped>
.plan-spinner {
  width: 56px;
  height: 56px;
  border: 5px solid #e8e8e8;
  border-top-color: #1a1a1a;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.plan-dot {
  display: block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #a8e6cf;
  border: 2px solid #1a1a1a;
  animation: bounce 0.75s ease-in-out infinite alternate;
}

@keyframes bounce {
  from { transform: translateY(0); opacity: 0.4; }
  to   { transform: translateY(-8px); opacity: 1; }
}
</style>
