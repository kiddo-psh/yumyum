<template>
  <header class="mb-6">
    <RouterLink
      to="/routine"
      class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
    >
      <span class="material-symbols-outlined text-base">chevron_left</span>
      <span>운동 루틴으로</span>
    </RouterLink>
    <h1 class="text-display-md text-on-background">운동 기록</h1>
  </header>

  <div class="md:grid md:grid-cols-[480px_1fr] md:gap-6 md:items-start">

    <!-- ── 캘린더 (왼쪽) ─────────────────────────── -->
    <div class="bg-white neo-brutal-border rounded-2xl p-5 mb-6 md:mb-0 md:sticky md:top-6">
      <!-- 월 네비게이션 -->
      <div class="flex items-center justify-between mb-5">
        <button
          class="w-11 h-11 flex items-center justify-center neo-brutal-border rounded-xl bg-surface hover:bg-nyam-mint/20 transition-colors"
          @click="prevMonth"
        >
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <span class="text-headline-md text-on-background font-bold">{{ currentYear }}년 {{ currentMonth }}월</span>
        <button
          class="w-11 h-11 flex items-center justify-center neo-brutal-border rounded-xl bg-surface hover:bg-nyam-mint/20 transition-colors"
          :disabled="isCurrentMonth"
          :class="isCurrentMonth ? 'opacity-30 cursor-not-allowed' : ''"
          @click="nextMonth"
        >
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
      </div>

      <!-- 요일 헤더 -->
      <div class="grid grid-cols-7 mb-2">
        <span
          v-for="d in ['일','월','화','수','목','금','토']"
          :key="d"
          class="text-body-sm text-on-surface-variant text-center py-1.5"
          :class="d === '일' ? 'text-danger' : d === '토' ? 'text-primary' : ''"
        >{{ d }}</span>
      </div>

      <!-- 날짜 그리드 -->
      <div class="grid grid-cols-7 gap-1">
        <div v-for="(cell, idx) in calendarCells" :key="idx" class="aspect-square">
          <div v-if="!cell" />
          <button
            v-else
            class="w-full h-full flex flex-col items-center justify-center rounded-xl transition-colors relative"
            :class="cellClass(cell)"
            @click="selectCell(cell)"
          >
            <span class="text-body-lg leading-none">{{ cell.d }}</span>
            <span
              v-if="cell.session"
              class="absolute bottom-1.5 w-2 h-2 rounded-full"
              :class="cell.dateStr === selectedDate ? 'bg-white' : 'bg-primary'"
            />
          </button>
        </div>
      </div>

      <div v-if="loading" class="flex items-center justify-center gap-2 text-on-surface-variant mt-3 text-label-sm">
        <span class="material-symbols-outlined text-base animate-spin">progress_activity</span>
        불러오는 중...
      </div>

      <!-- 이달 통계 -->
      <div v-if="!loading && sessions.length" class="mt-4 pt-4 border-t border-outline flex gap-4">
        <div class="flex-1 text-center">
          <p class="text-display-sm text-primary font-bold">{{ sessions.length }}</p>
          <p class="text-label-sm text-on-surface-variant">이달 운동</p>
        </div>
        <div class="flex-1 text-center">
          <p class="text-display-sm text-primary font-bold">{{ totalCalories.toLocaleString() }}</p>
          <p class="text-label-sm text-on-surface-variant">총 칼로리</p>
        </div>
      </div>
    </div>

    <!-- ── 상세 (오른쪽) ──────────────────────────── -->
    <div>
      <!-- 날짜 선택 전 -->
      <div
        v-if="!selectedDate"
        class="bg-white neo-brutal-border rounded-2xl p-10 flex flex-col items-center text-center gap-3"
      >
        <span class="material-symbols-outlined text-5xl text-on-surface-variant" style="font-variation-settings:'FILL' 1;">calendar_month</span>
        <p class="text-body-lg text-on-surface-variant">날짜를 선택하면<br>운동 기록을 볼 수 있습니다.</p>
      </div>

      <!-- 선택된 날 기록 없음 -->
      <div
        v-else-if="!selectedSession"
        class="bg-white neo-brutal-border rounded-2xl p-10 flex flex-col items-center text-center gap-3"
      >
        <span class="material-symbols-outlined text-5xl text-on-surface-variant">fitness_center</span>
        <p class="text-body-lg text-on-surface-variant">{{ formatDateKo(selectedDate) }}<br>운동 기록이 없습니다.</p>
      </div>

      <!-- 세션 상세 -->
      <div v-else class="grid gap-4">
        <div class="flex items-center gap-2">
          <p class="text-headline-md text-on-background">{{ formatDateKo(selectedDate) }}</p>
        </div>

        <!-- 요약 -->
        <div class="bg-nyam-mint/20 neo-brutal-border rounded-xl p-4 grid grid-cols-2 gap-4">
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">local_fire_department</span>
            <div>
              <p class="text-label-sm text-on-surface-variant">소모 칼로리</p>
              <p class="text-headline-sm text-on-background font-bold">{{ selectedSession.caloriesBurned }} kcal</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">check_circle</span>
            <div>
              <p class="text-label-sm text-on-surface-variant">완료 세트</p>
              <p class="text-headline-sm text-on-background font-bold">{{ completedSets }} / {{ selectedSession.sets.length }}</p>
            </div>
          </div>
        </div>

        <!-- 운동별 기록 -->
        <div
          v-for="ex in groupedExercises"
          :key="ex.exerciseName"
          class="bg-white neo-brutal-border rounded-xl p-5"
        >
          <p class="text-body-lg font-bold text-on-background mb-3">{{ ex.exerciseName }}</p>
          <div class="grid gap-1.5">
            <div class="grid grid-cols-[2rem_1fr_1fr_1.5rem] gap-2 text-label-sm text-on-surface-variant px-1">
              <span class="text-center">#</span>
              <span class="text-center">횟수</span>
              <span class="text-center">무게</span>
              <span />
            </div>
            <div
              v-for="s in ex.sets"
              :key="s.setNumber"
              class="grid grid-cols-[2rem_1fr_1fr_1.5rem] gap-2 items-center rounded-lg px-1 py-1.5"
              :class="s.completed ? 'bg-nyam-mint/20' : 'bg-surface'"
            >
              <span class="text-label-sm text-on-surface-variant text-center">{{ s.setNumber }}</span>
              <span class="text-body-md text-on-background text-center">{{ s.actualReps }}회</span>
              <span class="text-body-md text-on-background text-center">{{ s.actualWeightKg }}kg</span>
              <span
                class="material-symbols-outlined text-base text-center"
                :style="{ 'font-variation-settings': `'FILL' ${s.completed ? 1 : 0}` }"
                :class="s.completed ? 'text-success' : 'text-on-surface-variant'"
              >check_circle</span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getMonthSessions } from '@/api/routine'

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)

const sessions = ref([])
const loading = ref(false)
const selectedDate = ref('')

const todayStr = [
  today.getFullYear(),
  String(today.getMonth() + 1).padStart(2, '0'),
  String(today.getDate()).padStart(2, '0'),
].join('-')

// ── 캘린더 ─────────────────────────────────────
const isCurrentMonth = computed(
  () => currentYear.value === today.getFullYear() && currentMonth.value === today.getMonth() + 1
)

const calendarCells = computed(() => {
  const y = currentYear.value
  const m = currentMonth.value
  const firstWeekday = new Date(y, m - 1, 1).getDay()
  const daysInMonth = new Date(y, m, 0).getDate()
  const cells = []
  for (let i = 0; i < firstWeekday; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const session = sessions.value.find(s => s.sessionDate === dateStr) ?? null
    cells.push({ d, dateStr, session })
  }
  return cells
})

const totalCalories = computed(() => sessions.value.reduce((sum, s) => sum + s.caloriesBurned, 0))

function cellClass(cell) {
  if (cell.dateStr === selectedDate.value) return 'bg-primary text-white'
  if (cell.session) return 'bg-nyam-mint/30 text-on-background hover:bg-nyam-mint/60'
  if (cell.dateStr === todayStr) return 'ring-2 ring-primary text-primary hover:bg-surface'
  return 'text-on-background hover:bg-surface'
}

// ── 선택된 날 ────────────────────────────────────
const selectedSession = computed(() =>
  sessions.value.find(s => s.sessionDate === selectedDate.value) ?? null
)

const completedSets = computed(() =>
  selectedSession.value?.sets.filter(s => s.completed).length ?? 0
)

const groupedExercises = computed(() => {
  if (!selectedSession.value) return []
  const map = new Map()
  for (const s of selectedSession.value.sets) {
    if (!map.has(s.exerciseName)) map.set(s.exerciseName, [])
    map.get(s.exerciseName).push(s)
  }
  return Array.from(map.entries()).map(([exerciseName, sets]) => ({ exerciseName, sets }))
})

// ── 이벤트 ──────────────────────────────────────
function prevMonth() {
  if (currentMonth.value === 1) { currentYear.value--; currentMonth.value = 12 }
  else currentMonth.value--
  selectedDate.value = ''
}
function nextMonth() {
  if (isCurrentMonth.value) return
  if (currentMonth.value === 12) { currentYear.value++; currentMonth.value = 1 }
  else currentMonth.value++
  selectedDate.value = ''
}
function selectCell(cell) {
  selectedDate.value = selectedDate.value === cell.dateStr ? '' : cell.dateStr
}
function formatDateKo(dateStr) {
  const [y, m, d] = dateStr.split('-')
  return `${y}년 ${parseInt(m)}월 ${parseInt(d)}일`
}

// ── 데이터 ──────────────────────────────────────
async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await getMonthSessions(currentYear.value, currentMonth.value)
  } finally {
    loading.value = false
  }
}

watch([currentYear, currentMonth], loadSessions)
onMounted(loadSessions)
</script>
