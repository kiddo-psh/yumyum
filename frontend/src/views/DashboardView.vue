<template>
  <div class="max-w-4xl mx-auto w-full">

    <!-- Header -->
    <header class="mb-8">
      <h1 class="text-display-md text-on-background">대시보드</h1>
      <p class="text-body-md text-on-surface-variant mt-1">{{ todayFormatted }}</p>
    </header>

    <!-- Program 진행 현황 -->
    <div v-if="program" class="bg-white neo-brutal-border rounded-xl p-7 mb-6">
      <div class="flex items-start justify-between mb-5">
        <div>
          <h2 class="text-headline-lg font-bold text-on-background">Program 진행</h2>
          <p class="text-body-md text-on-surface-variant mt-1">{{ goalLabel }}</p>
        </div>
        <div class="px-4 py-2 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold shrink-0">
          {{ programWeekLabel }}
        </div>
      </div>
      <div class="h-7 bg-surface neo-brutal-border rounded-xl overflow-hidden mb-3">
        <div class="h-full bg-primary transition-all duration-700" :style="{ width: programProgress + '%' }" />
      </div>
      <div class="flex items-center justify-between">
        <span class="text-label-lg text-on-surface-variant">{{ program.startDate }}</span>
        <span class="text-body-md font-bold text-on-background">{{ programProgress }}% 완료</span>
        <span class="text-label-lg text-on-surface-variant">{{ program.endDate }}</span>
      </div>
    </div>

    <!-- 주간 달성 현황 (full width) -->
    <div class="bg-white neo-brutal-border rounded-xl p-7 mb-6">

      <!-- 헤더: 제목 + 주차 네비 + 달성률 한 줄 -->
      <div class="flex items-center justify-between mb-7">
        <div class="flex items-center gap-5">
          <h2 class="text-headline-lg font-bold text-on-background">주간 달성 현황</h2>
          <!-- 주차 네비게이션 -->
          <div class="flex items-center bg-surface neo-brutal-border rounded-xl overflow-hidden">
            <button
              class="w-10 h-10 flex items-center justify-center hover:bg-white transition-colors"
              @click="prevWeek"
            >
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <span class="text-label-lg font-bold px-3 min-w-[64px] text-center">{{ weekLabel }}</span>
            <button
              class="w-10 h-10 flex items-center justify-center transition-colors"
              :class="weekOffset >= 0 ? 'opacity-30 cursor-not-allowed' : 'hover:bg-white'"
              :disabled="weekOffset >= 0"
              @click="nextWeek"
            >
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
        <!-- 이번 주 달성률 -->
        <div class="flex items-baseline gap-1">
          <span class="text-numeral-xl text-primary leading-none">{{ weekAchievementRate }}</span>
          <span class="text-headline-md text-on-surface-variant pb-1">%</span>
        </div>
      </div>

      <!-- 7일 그리드 -->
      <div v-if="state.calendarLoading" class="flex items-center gap-3 text-on-surface-variant py-6">
        <span class="material-symbols-outlined animate-spin">progress_activity</span>
        <span class="text-body-md">불러오는 중...</span>
      </div>

      <div v-else class="grid grid-cols-7 gap-3">
        <div
          v-for="day in weekDays"
          :key="day.date"
          class="flex flex-col items-center gap-3"
        >
          <!-- 요일 -->
          <span
            class="text-label-lg font-bold"
            :class="day.isToday ? 'text-primary' : 'text-on-surface-variant'"
          >{{ day.label }}</span>

          <!-- 상태 배지 -->
          <div
            class="w-full aspect-square rounded-xl neo-brutal-border flex items-center justify-center"
            :class="
              day.achieved === true  ? 'bg-success' :
              day.achieved === false ? 'bg-danger'  : 'bg-surface'
            "
          >
            <span
              class="material-symbols-outlined text-2xl"
              :class="day.achieved !== null ? 'text-white' : 'text-on-surface-variant opacity-20'"
              style="font-variation-settings:'FILL' 1;"
            >{{ day.achieved === true ? 'check' : day.achieved === false ? 'close' : 'remove' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 체중 추세 -->
    <div class="bg-white neo-brutal-border rounded-xl p-7">

      <!-- 헤더 -->
      <div class="flex items-start justify-between mb-7">
        <div>
          <h2 class="text-headline-lg font-bold text-on-background">체중 추세</h2>
          <p v-if="latestWeight" class="text-body-md text-on-surface-variant mt-1">
            최근 <span class="font-bold text-on-background">{{ latestWeight }} kg</span>
          </p>
        </div>
        <!-- 일/주 토글 -->
        <div class="flex neo-brutal-border rounded-xl overflow-hidden shrink-0">
          <button
            class="px-5 py-2.5 text-label-lg font-bold transition-colors"
            :class="viewMode === 'daily' ? 'bg-on-background text-white' : 'bg-white hover:bg-surface'"
            @click="viewMode = 'daily'"
          >일</button>
          <button
            class="px-5 py-2.5 text-label-lg font-bold border-l-[3px] border-on-background transition-colors"
            :class="viewMode === 'weekly' ? 'bg-on-background text-white' : 'bg-white hover:bg-surface'"
            @click="viewMode = 'weekly'"
          >주</button>
        </div>
      </div>

      <div v-if="state.weightsLoading" class="flex items-center gap-3 text-on-surface-variant py-10">
        <span class="material-symbols-outlined animate-spin">progress_activity</span>
        <span class="text-body-md">불러오는 중...</span>
      </div>

      <div v-else-if="!weightPoints.length" class="py-12 text-center text-on-surface-variant">
        <span class="material-symbols-outlined text-5xl block mb-3 opacity-30" style="font-variation-settings:'FILL' 1;">monitor_weight</span>
        <p class="text-body-lg">아직 체중 기록이 없어요.</p>
        <p class="text-label-lg opacity-60 mt-1">체중을 기록하면 추세 그래프가 표시됩니다.</p>
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

        <!-- 체중 통계 -->
        <div class="grid grid-cols-3 gap-4 mt-7 pt-6 border-t-[3px] border-on-background">
          <div class="text-center">
            <p class="text-label-lg text-on-surface-variant mb-1">최저</p>
            <p class="text-headline-lg font-bold text-on-background">
              {{ weightMin }}<span class="text-body-md font-normal ml-0.5">kg</span>
            </p>
          </div>
          <div class="text-center">
            <p class="text-label-lg text-on-surface-variant mb-1">평균</p>
            <p class="text-headline-lg font-bold text-primary">
              {{ weightAvg }}<span class="text-body-md font-normal ml-0.5">kg</span>
            </p>
          </div>
          <div class="text-center">
            <p class="text-label-lg text-on-surface-variant mb-1">최고</p>
            <p class="text-headline-lg font-bold text-on-background">
              {{ weightMax }}<span class="text-body-md font-normal ml-0.5">kg</span>
            </p>
          </div>
        </div>
      </template>

      <!-- 체중 입력 폼 -->
      <div class="border-t-[3px] border-on-background mt-7 pt-6">
        <p v-if="weightSubmitError" class="text-danger text-label-lg font-bold mb-3">{{ weightSubmitError }}</p>
        <form class="flex gap-3" @submit.prevent="submitWeight">
          <input
            v-model.number="newWeight"
            type="number"
            step="0.1"
            placeholder="오늘 체중 (kg)"
            class="flex-1 neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary"
            required
          />
          <button
            type="submit"
            class="px-6 py-3 bg-primary text-white neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-all duration-150 disabled:opacity-50"
            :disabled="submitting"
          >
            {{ submitting ? '저장 중' : '기록' }}
          </button>
        </form>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'

import {
  getWeeklyCalendar,
  getWeightHistory,
} from '@/api/dashboard'
import { getCurrentProgram } from '@/api/my'
import { apiClient } from '@/services/apiClient'
import { getEffectiveToday } from '@/utils/effectiveDate'

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']
const CHART_W = 500
const CHART_H = 190
const CHART_PAD_L = 12
const CHART_PAD_R = 16
const CHART_PAD_T = 12
const CHART_PAD_B = 28

const weekOffset = ref(0)
const program = ref(null)

const GOAL_LABELS = { DIET: '다이어트', MUSCLE: '근육 증가', HEALTH: '건강 유지', DISEASE: '질환 관리' }

const goalLabel = computed(() => {
  if (!program.value) return ''
  return GOAL_LABELS[program.value.healthGoal] ?? program.value.healthGoal ?? ''
})

const programWeekLabel = computed(() => {
  if (!program.value) return ''
  const start = new Date(program.value.startDate + 'T00:00:00')
  const diffDays = Math.floor((getEffectiveToday() - start) / (1000 * 60 * 60 * 24))
  const currentWeek = Math.min(Math.floor(diffDays / 7) + 1, program.value.durationWeeks)
  return `${currentWeek}주차 / ${program.value.durationWeeks}주`
})

const programProgress = computed(() => {
  if (!program.value) return 0
  const start = new Date(program.value.startDate + 'T00:00:00')
  const end = new Date(program.value.endDate + 'T00:00:00')
  const now = getEffectiveToday()
  const total = end - start
  const elapsed = now - start
  return Math.min(100, Math.max(0, Math.round((elapsed / total) * 100)))
})

const state = reactive({
  calendarLoading: true,
  weeklyCalendar: null,
  weightsLoading: true,
  weights: [],
})

const viewMode = ref('daily')

const today = formatDate(getEffectiveToday())
const todayFormatted = formatDisplayDate(new Date())

const newWeight = ref(null)
const submitting = ref(false)
const weightSubmitError = ref('')

// ─── 주간 달성 현황 ───────────────────────────────────────────────

const weekLabel = computed(() => {
  if (weekOffset.value === 0) return '이번 주'
  if (weekOffset.value === -1) return '지난 주'
  return `${Math.abs(weekOffset.value)}주 전`
})

const weekDays = computed(() => {
  const days = state.weeklyCalendar?.days
  if (!days?.length) {
    return DAY_LABELS.map((label, i) => ({
      date: `empty-${i}`,
      label,
      dayNum: '—',
      achieved: null,
      rateLabel: '—',
      isToday: false,
    }))
  }

  return days.map((day) => {
    const rate = day.achievementRate ?? 0
    const hasData = rate > 0
    const date = new Date(`${day.date}T00:00:00`)

    return {
      date: day.date,
      label: DAY_LABELS[date.getDay()],
      dayNum: date.getDate(),
      achieved: hasData ? rate >= 0.9 && rate <= 1.1 : null,
      rateLabel: hasData ? `${Math.round(rate * 100)}%` : '—',
      isToday: day.date === today,
    }
  })
})

const weekAchievementRate = computed(() => {
  const days = state.weeklyCalendar?.days
  if (!days?.length) return 0
  const withData = days.filter(d => (d.achievementRate ?? 0) > 0)
  if (!withData.length) return 0
  const achieved = withData.filter(d => {
    const r = d.achievementRate ?? 0
    return r >= 0.9 && r <= 1.1
  }).length
  return Math.round((achieved / withData.length) * 100)
})

const achievedDays = computed(() =>
  (state.weeklyCalendar?.days ?? []).filter(d => {
    const r = d.achievementRate ?? 0
    return r >= 0.9 && r <= 1.1
  }).length
)

const failedDays = computed(() =>
  (state.weeklyCalendar?.days ?? []).filter(d => {
    const r = d.achievementRate ?? 0
    return r > 0 && (r < 0.9 || r > 1.1)
  }).length
)

const noDataDays = computed(() =>
  (state.weeklyCalendar?.days ?? []).filter(d => (d.achievementRate ?? 0) === 0).length
)

// ─── 체중 차트 ────────────────────────────────────────────────────

const weeklyWeights = computed(() => {
  if (!state.weights.length) return []
  const groups = new Map()
  state.weights.forEach(w => {
    const d = new Date(`${w.date}T00:00:00`)
    const monday = new Date(d)
    monday.setDate(d.getDate() - ((d.getDay() + 6) % 7))
    const key = formatDate(monday)
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(w.weight)
  })
  return [...groups.entries()].map(([date, weights]) => ({
    date,
    weight: Math.round((weights.reduce((s, v) => s + v, 0) / weights.length) * 10) / 10,
  }))
})

const displayWeights = computed(() =>
  viewMode.value === 'weekly' ? weeklyWeights.value : state.weights
)

const weightMin = computed(() => {
  if (!displayWeights.value.length) return 0
  return Math.min(...displayWeights.value.map(w => w.weight))
})

const weightMax = computed(() => {
  if (!displayWeights.value.length) return 0
  return Math.max(...displayWeights.value.map(w => w.weight))
})

const weightAvg = computed(() => {
  if (!displayWeights.value.length) return 0
  const avg = displayWeights.value.reduce((s, w) => s + w.weight, 0) / displayWeights.value.length
  return Math.round(avg * 10) / 10
})

const latestWeight = computed(() => state.weights.at(-1)?.weight ?? null)

const chartYMin = computed(() => Math.max(0, (weightMin.value ?? 0) - 2))
const chartYMax = computed(() => (weightMax.value ?? 0) + 2)

function toSvgY(v) {
  const range = chartYMax.value - chartYMin.value
  if (range === 0) return CHART_PAD_T + (CHART_H - CHART_PAD_T - CHART_PAD_B) / 2
  return CHART_PAD_T + (1 - (v - chartYMin.value) / range) * (CHART_H - CHART_PAD_T - CHART_PAD_B)
}

function toSvgX(i, total) {
  const usable = CHART_W - CHART_PAD_L - CHART_PAD_R
  return CHART_PAD_L + (total <= 1 ? usable / 2 : (i / (total - 1)) * usable)
}

const weightPoints = computed(() =>
  displayWeights.value.map((w, i) => ({
    x: toSvgX(i, displayWeights.value.length),
    y: toSvgY(w.weight),
    weight: w.weight,
    date: w.date,
  }))
)

const xLabels = computed(() => {
  const pts = weightPoints.value
  const weights = displayWeights.value
  if (!pts.length) return []
  if (pts.length <= 4) {
    return pts.map((p, i) => ({ x: p.x, label: formatShortDate(weights[i].date) }))
  }
  const indices = [0, Math.floor((pts.length - 1) / 2), pts.length - 1]
  return indices.map(i => ({ x: pts[i].x, label: formatShortDate(weights[i].date) }))
})

function smoothPath(points) {
  if (points.length < 2) return ''
  if (points.length === 2) return `M ${points[0].x},${points[0].y} L ${points[1].x},${points[1].y}`
  let d = `M ${points[0].x},${points[0].y}`
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[Math.max(0, i - 1)]
    const p1 = points[i]
    const p2 = points[i + 1]
    const p3 = points[Math.min(points.length - 1, i + 2)]
    const cp1x = p1.x + (p2.x - p0.x) / 6
    const cp1y = p1.y + (p2.y - p0.y) / 6
    const cp2x = p2.x - (p3.x - p1.x) / 6
    const cp2y = p2.y - (p3.y - p1.y) / 6
    d += ` C ${cp1x.toFixed(1)},${cp1y.toFixed(1)} ${cp2x.toFixed(1)},${cp2y.toFixed(1)} ${p2.x},${p2.y}`
  }
  return d
}

function fillPath(points) {
  if (points.length < 2) return ''
  const bottom = CHART_H - CHART_PAD_B
  return `${smoothPath(points)} L ${points[points.length - 1].x},${bottom} L ${points[0].x},${bottom} Z`
}

// ─── 이벤트 핸들러 ────────────────────────────────────────────────

function prevWeek() { weekOffset.value -= 1 }
function nextWeek() {
  if (weekOffset.value >= 0) return
  weekOffset.value += 1
}

async function loadCalendar() {
  state.calendarLoading = true
  try {
    state.weeklyCalendar = await getWeeklyCalendar(weekOffset.value)
  } catch {
    state.weeklyCalendar = null
  } finally {
    state.calendarLoading = false
  }
}

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

async function submitWeight() {
  submitting.value = true
  weightSubmitError.value = ''
  try {
    const created = await apiClient.post('/weights', {
      weightKg: newWeight.value,
      recordedDate: today,
    })
    state.weights = [...state.weights.filter(w => w.date !== created.recordedDate),
                     { id: created.id, date: created.recordedDate, weight: created.weightKg }]
      .sort((a, b) => a.date.localeCompare(b.date))
    newWeight.value = null
  } catch {
    weightSubmitError.value = '체중 기록에 실패했어요.'
  } finally {
    submitting.value = false
  }
}

// ─── 초기화 ───────────────────────────────────────────────────────

watch(weekOffset, loadCalendar)

onMounted(() => {
  loadCalendar()
  loadWeights()
  getCurrentProgram().then(p => { program.value = p }).catch(() => {})
})

// ─── 유틸 ─────────────────────────────────────────────────────────

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatDisplayDate(date) {
  const m = date.getMonth() + 1
  const d = date.getDate()
  const day = DAY_LABELS[date.getDay()]
  return `${date.getFullYear()}년 ${m}월 ${d}일 (${day})`
}

function formatShortDate(dateStr) {
  const d = new Date(`${dateStr}T00:00:00`)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>
