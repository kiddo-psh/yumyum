<template>
  <div class="max-w-4xl mx-auto w-full">

    <!-- Header -->
    <header class="flex justify-between items-start mb-8 w-full">
      <div>
        <h2 class="text-headline-lg text-on-background">안녕! 오늘 하루는 어때요?</h2>
        <div v-if="hasRecommendation" class="flex items-center gap-3 mt-1 flex-wrap">
          <p class="text-body-md text-on-surface-variant">
            <span class="font-bold text-on-background">{{ recommendation.name }}</span> 어때요?
            {{ recommendation.reason }}
          </p>
          <RouterLink
            :to="{ name: 'meal-manual', query: { q: recommendation.name } }"
            class="shrink-0 bg-primary text-on-primary px-4 py-1.5 neo-brutal-border rounded-lg text-label-lg hover:-translate-y-0.5 transition-transform"
          >
            식단에 추가
          </RouterLink>
        </div>
        <p
          v-else-if="state.balance?.lastMealRecommendTrigger && state.recommendLoading"
          class="text-body-md text-on-surface-variant mt-1"
        >
          마지막 끼니 추천을 가져오는 중...
        </p>
        <p v-else class="text-body-md text-on-surface-variant mt-1">{{ coachMessage }}</p>
      </div>
      <div class="flex gap-3 flex-shrink-0">
        <button aria-label="알림" class="w-11 h-11 flex items-center justify-center neo-brutal-border rounded-xl hover:bg-surface transition-colors">
          <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
        </button>
        <RouterLink to="/my" aria-label="설정" class="w-11 h-11 flex items-center justify-center neo-brutal-border rounded-xl hover:bg-surface transition-colors">
          <span class="material-symbols-outlined" aria-hidden="true">settings</span>
        </RouterLink>
      </div>
    </header>

    <!-- 플랜 생성 대기 -->
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

    <template v-if="isProgramReady">

      <!-- Row 1: 냠냠이 상태 + 스트릭/주간 -->
      <div class="grid grid-cols-1 lg:grid-cols-5 gap-4 mb-4">

        <!-- 냠냠이 카드 -->
        <div class="lg:col-span-3 bg-nyam-mint neo-brutal-border rounded-xl p-5 flex items-center gap-5">
          <div class="w-24 h-24 bg-white neo-brutal-border rounded-xl flex items-center justify-center flex-shrink-0 overflow-hidden">
            <img src="/nyam/nyamnyam.png" alt="냠냠이" class="w-20 h-20 object-contain" />
          </div>
          <div class="min-w-0">
            <p class="text-label-lg text-on-background">냠냠이 현황</p>
            <h3 class="text-headline-md text-on-background mt-1 leading-snug">{{ nyamStatusMessage }}</h3>
          </div>
        </div>

        <!-- 스트릭 + 주간 7도트 카드 -->
        <div class="lg:col-span-2 bg-white neo-brutal-border rounded-xl p-5 flex flex-col gap-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-warning neo-brutal-border rounded-xl flex items-center justify-center text-xl flex-shrink-0">🔥</div>
            <div>
              <p class="text-headline-lg text-on-background leading-none">
                {{ currentStreak }}<span class="text-body-md text-on-surface-variant ml-1">일</span>
              </p>
              <p class="text-label-lg text-on-surface-variant">연속 달성 중</p>
            </div>
          </div>

          <!-- 7도트 주간 캘린더 -->
          <div class="flex justify-between items-end gap-1.5">
            <div
              v-for="dot in weekDots"
              :key="dot.label"
              class="flex flex-col items-center gap-1.5 flex-1"
            >
              <div
                class="w-7 h-7 rounded-full transition-all duration-300"
                :class="dotClass(dot.state)"
              />
              <span class="text-xs text-on-surface-variant">{{ dot.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Row 2: 칼로리 + 아크 게이지 + 매크로 -->
      <div class="bg-surface neo-brutal-border rounded-xl p-6 mb-4">

        <!-- 칼로리 숫자 + 반원 게이지 -->
        <div class="flex flex-col md:flex-row items-center gap-6 mb-5">

          <!-- 칼로리 수치 -->
          <div class="flex-1 text-center md:text-left">
            <p class="text-label-lg text-on-surface-variant mb-2">오늘의 칼로리</p>
            <div class="flex items-baseline gap-2 justify-center md:justify-start">
              <span class="text-numeral-xl text-on-background">{{ formatNumber(intakeCalories) }}</span>
              <span class="text-headline-md text-on-surface-variant">kcal</span>
            </div>
            <p class="text-body-md text-on-surface-variant mt-2">
              목표 <span class="font-bold text-on-background">{{ formatNumber(targetCalories) }}</span> kcal 중
            </p>
            <p class="text-label-lg text-primary mt-1">
              {{ formatNumber(remainingCalories) }} kcal 남음
            </p>
          </div>

          <!-- 반원 아크 게이지 -->
          <div class="w-52 flex-shrink-0">
            <svg
              viewBox="0 0 200 115"
              class="w-full"
              role="img"
              :aria-label="`오늘 칼로리 달성률 ${calorieProgress}%`"
            >
              <title>오늘 칼로리 달성률 {{ calorieProgress }}%</title>
              <!-- 배경 트랙 -->
              <path
                d="M 20,100 A 80,80 0 0,0 180,100"
                fill="none"
                stroke="#E0E0E0"
                stroke-width="16"
                stroke-linecap="round"
              />
              <!-- 진행 아크 -->
              <path
                d="M 20,100 A 80,80 0 0,0 180,100"
                fill="none"
                stroke="#FF8C42"
                stroke-width="16"
                stroke-linecap="round"
                :stroke-dasharray="ARC_LENGTH"
                :stroke-dashoffset="arcOffset"
                style="transition: stroke-dashoffset 1.5s cubic-bezier(0.22, 1, 0.36, 1);"
              />
              <!-- 퍼센트 텍스트 -->
              <text
                x="100"
                y="76"
                text-anchor="middle"
                fill="#2D2D2D"
                font-size="30"
                font-weight="800"
                font-family="inherit"
              >{{ calorieProgress }}</text>
              <text
                x="100"
                y="100"
                text-anchor="middle"
                fill="#8A8A8A"
                font-size="14"
                font-weight="700"
                font-family="inherit"
              >%</text>
            </svg>
          </div>
        </div>

        <!-- 가로 매크로 바 -->
        <div class="space-y-3 pt-5 border-t-[3px] border-on-background">
          <div
            v-for="macro in macros"
            :key="macro.key"
            class="flex items-center gap-3"
          >
            <span class="text-label-lg text-on-surface-variant w-16 flex-shrink-0 text-right">{{ macro.label }}</span>
            <div class="flex-1 h-3 bg-white neo-brutal-border rounded-full overflow-hidden">
              <div
                class="h-full border-r-[3px] border-on-background transition-all duration-700"
                :class="macro.colorClass"
                :style="{ width: macro.percent + '%' }"
              />
            </div>
            <span class="text-label-lg text-on-surface-variant w-24 text-right flex-shrink-0">
              {{ formatNumber(macro.current) }}g <span class="text-on-surface-variant/60">/ {{ macro.target }}g</span>
            </span>
          </div>
        </div>
      </div>

      <!-- Row 3: 퀵 액션 -->
      <div class="space-y-3">
        <div class="grid grid-cols-3 gap-3">
          <RouterLink
            to="/meals/search"
            class="col-span-2 bg-primary text-on-primary neo-brutal-border rounded-xl p-5 flex items-center gap-4 neo-brutal-shadow hover:-translate-y-0.5 transition-all duration-200"
          >
            <span class="material-symbols-outlined text-4xl flex-shrink-0" style="font-variation-settings:'FILL' 1;">edit_note</span>
            <div>
              <p class="text-label-lg">식단 기록</p>
              <p class="text-body-md text-on-primary/70 mt-0.5">오늘 먹은 걸 기록해요</p>
            </div>
          </RouterLink>
          <RouterLink
            to="/meals/photo"
            class="bg-white neo-brutal-border rounded-xl p-5 flex flex-col items-center justify-center text-center gap-2 hover:-translate-y-0.5 transition-all duration-200"
          >
            <span class="material-symbols-outlined text-3xl text-primary" style="font-variation-settings:'FILL' 1;">camera_enhance</span>
            <span class="text-label-lg text-on-background">사진 분석</span>
          </RouterLink>
        </div>

        <!-- 체중 위젯 -->
        <div class="bg-white neo-brutal-border rounded-xl p-5">
          <div class="flex items-center gap-2 mb-3">
            <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">monitor_weight</span>
            <span class="text-label-lg text-on-background">오늘 체중</span>
            <span v-if="weightSuccess" class="ml-auto text-success text-label-lg flex items-center gap-1">
              <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL' 1;">check_circle</span>기록 완료
            </span>
          </div>
          <p v-if="weightError" class="text-label-lg text-danger mb-2">{{ weightError }}</p>
          <form class="flex gap-2" @submit.prevent="submitWeight">
            <input
              v-model.number="newWeight"
              type="number"
              step="0.1"
              placeholder="오늘 체중 (kg)"
              aria-label="오늘 체중 (kg)"
              class="flex-1 border-2 border-outline rounded-[10px] px-3 py-2.5 text-body-md text-on-background bg-background focus:border-on-background focus:outline-none transition-colors"
              required
            />
            <button
              type="submit"
              class="px-5 py-2.5 bg-primary text-on-primary neo-brutal-border rounded-[10px] text-label-lg disabled:opacity-40 hover:-translate-y-0.5 transition-all duration-150"
              :disabled="weightSubmitting"
              :aria-busy="weightSubmitting"
              :aria-label="weightSubmitting ? '기록 중' : '체중 기록'"
            >
              {{ weightSubmitting ? '...' : '기록' }}
            </button>
          </form>
        </div>
      </div>

    </template>
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

const ARC_LENGTH = Math.PI * 80

const state = reactive({
  loading: true,
  summary: null,
  balance: null,
  recommendLoading: false,
  recommendData: null,
})

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

const arcOffset = computed(() => ARC_LENGTH * (1 - calorieProgress.value / 100))

const isProgramReady = computed(() => (state.balance?.targetCalories ?? 0) > 0)

const coachMessage = computed(() => {
  if (!isProgramReady.value) return 'AI가 맞춤 플랜을 생성하고 있어요...'
  if (aiComment.value) return aiComment.value
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 기록해볼까요?`
})

const nyamStatusMessage = computed(() => {
  const streak = currentStreak.value
  const progress = calorieProgress.value
  if (streak >= 30) return '30일 연속 달성! 전설이에요!'
  if (streak >= 7) return '일주일 연속 달성! 훌륭해요!'
  if (progress >= 100) return '오늘 목표 달성! 완벽해요!'
  if (progress >= 80) return '거의 다 왔어요. 조금만 더!'
  if (progress >= 50) return '반환점 돌파! 화이팅!'
  if (streak > 0) return `${streak}일 연속 중이에요!`
  return '오늘도 함께 냠냠해요!'
})

const weekDots = computed(() => {
  const todayDate = getEffectiveToday()
  const todayDay = todayDate.getDay() // 0=Sun, 1=Mon...6=Sat
  const todayIdx = todayDay === 0 ? 6 : todayDay - 1 // Mon=0...Sun=6
  const streak = currentStreak.value
  const progress = calorieProgress.value

  return ['월', '화', '수', '목', '금', '토', '일'].map((label, i) => {
    const offsetFromToday = i - todayIdx
    if (offsetFromToday > 0) return { label, state: 'future' }
    if (offsetFromToday === 0) {
      return { label, state: progress >= 80 ? 'today-achieved' : 'today-pending' }
    }
    const daysAgo = -offsetFromToday
    return { label, state: daysAgo <= streak ? 'achieved' : 'missed' }
  })
})

const macros = computed(() => [
  { key: 'carbs',   label: '탄수화물', current: state.summary?.totalCarbs ?? 0,   target: 250, colorClass: 'bg-carbs',   percent: clamp(Math.round(((state.summary?.totalCarbs ?? 0) / 250) * 100)) },
  { key: 'protein', label: '단백질',   current: state.summary?.totalProtein ?? 0, target: 120, colorClass: 'bg-protein', percent: clamp(Math.round(((state.summary?.totalProtein ?? 0) / 120) * 100)) },
  { key: 'fat',     label: '지방',     current: state.summary?.totalFat ?? 0,     target: 60,  colorClass: 'bg-fat',     percent: clamp(Math.round(((state.summary?.totalFat ?? 0) / 60) * 100)) },
])

const hasRecommendation = computed(() => Boolean(state.recommendData?.recommendations?.[0]))

const recommendation = computed(() => {
  const rec = state.recommendData?.recommendations?.[0]
  return { name: rec?.name ?? '', reason: rec?.reason ?? '잔여 영양소 기반 AI 추천' }
})

function dotClass(dotState) {
  switch (dotState) {
    case 'achieved':       return 'bg-primary neo-brutal-border'
    case 'today-achieved': return 'bg-primary border-[3px] border-on-background shadow-[3px_3px_0_0_#2D2D2D]'
    case 'today-pending':  return 'bg-white border-[3px] border-on-background shadow-[3px_3px_0_0_#2D2D2D]'
    case 'missed':         return 'bg-white border-2 border-outline opacity-50'
    case 'future':         return 'bg-surface border-2 border-outline opacity-30'
    default:               return 'bg-surface border-2 border-outline'
  }
}

onMounted(async () => {
  const [summary, balance] = await Promise.allSettled([
    getDailySummary(today),
    getCalorieBalance(today),
  ])

  state.summary = summary.status === 'fulfilled' ? summary.value : null
  state.balance = balance.status === 'fulfilled' ? balance.value : null
  state.loading = false

  if (state.balance?.lastMealRecommendTrigger) {
    loadRecommendation()
  }

  if (!state.balance?.targetCalories) {
    pollForProgram()
  }

  try {
    const res = await getHomeComment()
    aiComment.value = res.comment
  } catch {
    // coachMessage computed가 정적 fallback 반환
  }
})

async function pollForProgram(attempts = 0) {
  if (attempts >= 6) return
  await new Promise(r => setTimeout(r, 2000))
  try {
    const balance = await getCalorieBalance(today)
    if (balance?.targetCalories > 0) {
      state.balance = balance
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
    // recommendation not available
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

function clamp(v) { return Math.max(0, Math.min(100, v)) }

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
  border-top-color: #2D2D2D;
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
  border: 2px solid #2D2D2D;
  animation: plan-dot-rise 0.75s ease-in-out infinite alternate;
}

@keyframes plan-dot-rise {
  from { transform: translateY(0); opacity: 0.4; }
  to   { transform: translateY(-8px); opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .plan-spinner { animation: none; opacity: 0.5; }
  .plan-dot { animation: none; opacity: 0.7; }
}
</style>
