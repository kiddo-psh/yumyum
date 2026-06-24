<template>
  <!-- 헤더 -->
  <header class="mb-8">
    <RouterLink
      to="/routine"
      class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
    >
      <span class="material-symbols-outlined text-base">chevron_left</span>
      <span>운동 루틴으로</span>
    </RouterLink>

    <div v-if="routine" class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-display-md text-on-background">{{ routine.name }}</h1>
        <p class="text-body-md text-on-surface-variant mt-1">주 {{ routine.daysPerWeek }}회</p>
      </div>

      <div class="flex flex-col items-end gap-2 shrink-0">
        <span
          class="neo-brutal-border rounded-full px-3 py-1.5 text-label-lg"
          :class="routine.aiGenerated ? 'bg-nyam-mint text-on-background' : 'bg-surface text-on-surface-variant'"
        >
          {{ routine.aiGenerated ? 'AI 생성' : '직접 등록' }}
        </span>

        <!-- 모드별 버튼 -->
        <template v-if="!workoutMode">
          <button
            class="flex items-center gap-1 bg-primary text-white neo-brutal-border rounded-xl px-4 py-2 text-label-lg hover:-translate-y-0.5 transition-transform"
            @click="openSplitSelect"
          >
            <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL' 1;">play_circle</span>
            운동 시작
          </button>
        </template>
        <template v-else>
          <div class="flex items-center gap-2">
            <button
              class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors"
              @click="cancelWorkout"
            >
              취소
            </button>
            <button
              class="flex items-center gap-1 bg-success text-white neo-brutal-border rounded-xl px-4 py-2 text-label-lg hover:-translate-y-0.5 transition-transform disabled:opacity-40"
              :disabled="submitting"
              @click="finishWorkout"
            >
              <span v-if="submitting" class="material-symbols-outlined text-base animate-spin">progress_activity</span>
              <span v-else class="material-symbols-outlined text-base" style="font-variation-settings:'FILL' 1;">check_circle</span>
              운동 완료
            </button>
          </div>
        </template>
      </div>
    </div>
  </header>

  <!-- 로딩 / 에러 -->
  <div v-if="loading" class="flex items-center gap-3 text-on-surface-variant py-6">
    <span class="material-symbols-outlined animate-spin">progress_activity</span>
    <span>루틴을 불러오는 중...</span>
  </div>
  <div v-else-if="pageError" class="bg-white neo-brutal-border rounded-xl p-6 text-danger text-body-md font-bold">
    {{ pageError }}
  </div>

  <!-- 완료 배너 -->
  <div
    v-if="sessionDone"
    class="bg-nyam-mint neo-brutal-border rounded-xl p-5 mb-6"
  >
    <div class="flex items-center gap-3 mb-3">
      <span class="material-symbols-outlined text-2xl" style="font-variation-settings:'FILL' 1;">emoji_events</span>
      <div>
        <p class="text-body-lg font-bold text-on-background">운동 완료!</p>
        <p class="text-body-sm text-on-surface-variant">AI가 다음 주 루틴을 자동으로 조정합니다.</p>
      </div>
    </div>
    <div class="flex items-center gap-4 bg-white/60 rounded-xl px-4 py-3">
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">local_fire_department</span>
        <span class="text-body-md text-on-surface-variant">소모 칼로리</span>
      </div>
      <span class="text-headline-sm text-on-background font-bold ml-auto">{{ doneCalories }} kcal</span>
    </div>
  </div>

  <template v-if="routine && !loading && !pageError">
    <div v-if="routine.aiComment && !workoutMode" class="bg-nyam-mint/20 neo-brutal-border rounded-xl p-5 mb-8">
      <p class="text-body-md text-on-background leading-relaxed">{{ routine.aiComment }}</p>
    </div>

    <!-- ── 분할 선택 모달 ──────────────────────────── -->
    <div
      v-if="splitSelectOpen"
      class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40"
      @click.self="splitSelectOpen = false"
    >
      <div class="w-full sm:max-w-md bg-background neo-brutal-border rounded-t-2xl sm:rounded-2xl p-6">
        <p class="text-headline-md text-on-background mb-4">오늘 어떤 운동할까요?</p>
        <ul class="grid gap-3">
          <li v-for="day in uniqueDays" :key="day">
            <button
              class="w-full flex items-center justify-between neo-brutal-border rounded-xl px-5 py-4 bg-white hover:bg-nyam-mint/20 transition-colors"
              @click="startWorkoutForDay(day)"
            >
              <span class="text-body-lg font-bold text-on-background">{{ day }}</span>
              <div class="text-label-sm text-on-surface-variant text-right">
                <span>{{ exerciseCountForDay(day) }}가지 운동</span>
              </div>
            </button>
          </li>
        </ul>
        <button
          class="mt-4 w-full text-label-lg text-on-surface-variant hover:text-on-background py-2 transition-colors"
          @click="splitSelectOpen = false"
        >
          취소
        </button>
      </div>
    </div>

    <!-- ── 운동 모드 ──────────────────────────── -->
    <template v-if="workoutMode">
      <div class="flex items-center gap-2 mb-6">
        <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">fitness_center</span>
        <h2 class="text-headline-md text-on-background">{{ selectedDay }}</h2>
        <span class="ml-auto text-label-sm text-on-surface-variant">
          {{ completedSetCount }} / {{ totalSetCount }} 세트 완료
        </span>
      </div>

      <!-- 소모 칼로리 -->
      <div class="bg-white neo-brutal-border rounded-xl p-4 mb-6">
        <div class="flex items-center gap-3">
          <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">local_fire_department</span>
          <label class="text-body-md text-on-surface-variant shrink-0">소모 칼로리</label>
          <input
            v-model.number="caloriesBurned"
            type="number" min="0" step="10"
            class="flex-1 neo-brutal-border rounded-lg px-3 py-1.5 text-body-md text-right bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
            @input="isAutoCalc = false"
          />
          <span class="text-body-md text-on-surface-variant shrink-0">kcal</span>
        </div>
        <div class="flex items-center justify-between mt-2 px-1">
          <span class="text-label-sm text-on-surface-variant">
            {{ isAutoCalc ? '세트 완료 시 자동 계산' : `예상 ${estimatedCalories} kcal` }}
          </span>
          <button
            v-if="!isAutoCalc"
            class="text-label-sm text-primary hover:underline"
            @click="isAutoCalc = true; caloriesBurned = estimatedCalories"
          >
            자동 계산으로 되돌리기
          </button>
        </div>
      </div>

      <!-- 운동 카드 목록 -->
      <ul class="grid gap-4 mb-6">
        <li
          v-for="ex in selectedExercises"
          :key="ex.id"
          class="bg-white neo-brutal-border rounded-xl p-5"
        >
          <div class="flex items-center justify-between mb-3">
            <p class="text-body-lg font-bold text-on-background">{{ ex.exerciseName }}</p>
            <span class="text-label-sm text-on-surface-variant">
              목표 {{ ex.targetSets }}×{{ ex.targetReps }}회 · {{ ex.targetWeightKg }}kg
            </span>
          </div>

          <div class="grid gap-2">
            <div class="grid grid-cols-[2rem_1fr_1fr_2.5rem] gap-2 items-center text-label-sm text-on-surface-variant px-1">
              <span class="text-center">#</span>
              <span class="text-center">횟수</span>
              <span class="text-center">무게 (kg)</span>
              <span></span>
            </div>
            <div
              v-for="s in sessionSets[ex.id]"
              :key="s.setNumber"
              class="grid grid-cols-[2rem_1fr_1fr_2.5rem] gap-2 items-center"
            >
              <span class="text-label-sm text-on-surface-variant text-center">{{ s.setNumber }}</span>
              <input
                v-model.number="s.actualReps"
                type="number" min="0" step="1"
                class="neo-brutal-border rounded-lg px-2 py-1.5 text-body-md text-center focus:outline-none focus:ring-2 focus:ring-primary transition-colors"
                :class="s.completed ? 'bg-nyam-mint/30' : 'bg-surface'"
              />
              <input
                v-model.number="s.actualWeightKg"
                type="number" min="0" step="0.5"
                class="neo-brutal-border rounded-lg px-2 py-1.5 text-body-md text-center focus:outline-none focus:ring-2 focus:ring-primary transition-colors"
                :class="s.completed ? 'bg-nyam-mint/30' : 'bg-surface'"
              />
              <button
                class="w-9 h-9 rounded-lg neo-brutal-border flex items-center justify-center transition-colors"
                :class="s.completed ? 'bg-nyam-mint' : 'bg-white hover:bg-surface'"
                @click="s.completed = !s.completed"
              >
                <span
                  class="material-symbols-outlined text-base"
                  :style="{ 'font-variation-settings': `'FILL' ${s.completed ? 1 : 0}` }"
                  :class="s.completed ? 'text-on-background' : 'text-on-surface-variant'"
                >check</span>
              </button>
            </div>
          </div>
        </li>
      </ul>

      <!-- sticky 완료 버튼 -->
      <div class="sticky bottom-4 flex justify-center">
        <button
          class="flex items-center gap-2 bg-success text-white neo-brutal-border rounded-2xl px-8 py-4 text-headline-sm shadow-lg hover:-translate-y-0.5 transition-transform disabled:opacity-40"
          :disabled="submitting"
          @click="finishWorkout"
        >
          <span v-if="submitting" class="material-symbols-outlined animate-spin">progress_activity</span>
          <span v-else class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">check_circle</span>
          운동 완료
        </button>
      </div>
    </template>

    <!-- ── 편집 모드 ──────────────────────────── -->
    <template v-else>
      <div v-for="day in groupedDays" :key="day.dayLabel" class="mb-10">
        <h2 class="text-headline-md text-on-background mb-3 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">fitness_center</span>
          {{ day.dayLabel }}
        </h2>

        <ul class="grid gap-3 mb-3">
          <li
            v-for="ex in day.exercises"
            :key="ex.id"
            class="bg-white neo-brutal-border rounded-xl p-5"
          >
            <div class="flex items-center gap-2 mb-4">
              <input
                v-model="edits[ex.id].exerciseName"
                type="text"
                placeholder="운동 이름"
                class="flex-1 neo-brutal-border rounded-lg px-3 py-2 text-body-lg font-bold text-on-background bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <button
                class="shrink-0 text-on-surface-variant hover:text-danger transition-colors disabled:opacity-40"
                :disabled="deleting[ex.id]"
                @click="removeExercise(ex)"
              >
                <span v-if="deleting[ex.id]" class="material-symbols-outlined text-lg animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-lg">delete</span>
              </button>
            </div>

            <div class="grid grid-cols-3 gap-3">
              <div>
                <p class="text-label-sm text-on-surface-variant mb-1">세트</p>
                <input v-model.number="edits[ex.id].targetSets" type="number" min="1" step="1"
                  class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
              </div>
              <div>
                <p class="text-label-sm text-on-surface-variant mb-1">횟수</p>
                <input v-model.number="edits[ex.id].targetReps" type="number" min="1" step="1"
                  class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
              </div>
              <div>
                <p class="text-label-sm text-on-surface-variant mb-1">무게 (kg)</p>
                <input v-model.number="edits[ex.id].targetWeightKg" type="number" min="0" step="0.5"
                  class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
              </div>
            </div>

            <div v-if="isDirty(ex)" class="flex items-center gap-3 mt-3">
              <button
                class="flex items-center gap-1 bg-primary text-white neo-brutal-border rounded-lg px-4 py-2 text-label-lg disabled:opacity-40 hover:-translate-y-0.5 transition-transform"
                :disabled="saving[ex.id]"
                @click="save(ex)"
              >
                <span v-if="saving[ex.id]" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-sm">check</span>
                저장
              </button>
              <button class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors" @click="reset(ex)">
                취소
              </button>
            </div>
            <p v-if="saved[ex.id]" class="flex items-center gap-1 text-success text-label-sm mt-2">
              <span class="material-symbols-outlined text-sm" style="font-variation-settings:'FILL' 1;">check_circle</span>
              저장됨
            </p>
          </li>
        </ul>

        <button
          class="w-full flex items-center justify-center gap-2 border-2 border-dashed border-outline rounded-xl py-3 text-label-lg text-on-surface-variant hover:border-primary hover:text-primary transition-colors"
          @click="openAddForm(day.dayLabel)"
        >
          <span class="material-symbols-outlined text-lg">add</span>
          {{ day.dayLabel }} 운동 추가
        </button>

        <div v-if="addForm.dayLabel === day.dayLabel" class="mt-3 bg-white neo-brutal-border rounded-xl p-5">
          <p class="text-label-lg font-bold text-on-background mb-3">새 운동 추가</p>
          <input
            v-model="addForm.exerciseName"
            type="text"
            placeholder="운동 이름 (예: 스쿼트)"
            class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-on-background bg-surface focus:outline-none focus:ring-2 focus:ring-primary mb-3"
          />
          <div class="grid grid-cols-3 gap-3 mb-4">
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">세트</p>
              <input v-model.number="addForm.targetSets" type="number" min="1" step="1"
                class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
            </div>
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">횟수</p>
              <input v-model.number="addForm.targetReps" type="number" min="1" step="1"
                class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
            </div>
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">무게 (kg)</p>
              <input v-model.number="addForm.targetWeightKg" type="number" min="0" step="0.5"
                class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary" />
            </div>
          </div>
          <div class="flex gap-3">
            <button
              class="flex items-center gap-1 bg-primary text-white neo-brutal-border rounded-lg px-4 py-2 text-label-lg disabled:opacity-40"
              :disabled="!addForm.exerciseName.trim() || adding"
              @click="confirmAdd(day)"
            >
              <span v-if="adding" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
              추가
            </button>
            <button class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors" @click="closeAddForm">
              취소
            </button>
          </div>
        </div>
      </div>
    </template>
  </template>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getRoutine, updateExercise, addExercise, deleteExercise, recordSession } from '@/api/routine'

const route = useRoute()
const routineId = Number(route.params.routineId)

// ── 공통 ───────────────────────────────────────────
const routine = ref(null)
const loading = ref(false)
const pageError = ref('')

// ── 편집 모드 ──────────────────────────────────────
const edits = reactive({})
const saving = reactive({})
const saved = reactive({})
const deleting = reactive({})
const adding = ref(false)
const addForm = reactive({ dayLabel: '', exerciseName: '', targetSets: 3, targetReps: 10, targetWeightKg: 0 })

// ── 운동 모드 ──────────────────────────────────────
const splitSelectOpen = ref(false)
const workoutMode = ref(false)
const selectedDay = ref('')
const sessionSets = reactive({})
const caloriesBurned = ref(0)
const isAutoCalc = ref(true)
const submitting = ref(false)
const sessionDone = ref(false)
const doneCalories = ref(0)

// ── computed ───────────────────────────────────────
const groupedDays = computed(() => {
  if (!routine.value) return []
  const order = []
  const byLabel = new Map()
  for (const ex of routine.value.exercises) {
    if (!byLabel.has(ex.dayLabel)) {
      byLabel.set(ex.dayLabel, [])
      order.push(ex.dayLabel)
    }
    byLabel.get(ex.dayLabel).push(ex)
  }
  return order.map(dayLabel => ({ dayLabel, exercises: byLabel.get(dayLabel) }))
})

const uniqueDays = computed(() => groupedDays.value.map(d => d.dayLabel))

const selectedExercises = computed(() =>
  groupedDays.value.find(d => d.dayLabel === selectedDay.value)?.exercises ?? []
)

// estimatedCalories must come after selectedExercises to avoid TDZ crash
const estimatedCalories = computed(() => {
  const COEFF = 0.025
  return Math.round(
    selectedExercises.value
      .flatMap(ex => sessionSets[ex.id] ?? [])
      .filter(s => s.completed)
      .reduce((sum, s) => sum + s.actualWeightKg * s.actualReps * COEFF, 0)
  )
})

watch(estimatedCalories, val => {
  if (isAutoCalc.value) caloriesBurned.value = val
})

const totalSetCount = computed(() =>
  selectedExercises.value.reduce((sum, ex) => sum + (sessionSets[ex.id]?.length ?? 0), 0)
)

const completedSetCount = computed(() =>
  selectedExercises.value.reduce(
    (sum, ex) => sum + (sessionSets[ex.id]?.filter(s => s.completed).length ?? 0), 0
  )
)

function exerciseCountForDay(dayLabel) {
  return groupedDays.value.find(d => d.dayLabel === dayLabel)?.exercises.length ?? 0
}

// ── 편집 모드 함수 ─────────────────────────────────
function initEdit(ex) {
  edits[ex.id] = {
    exerciseName: ex.exerciseName,
    targetSets: ex.targetSets,
    targetReps: ex.targetReps,
    targetWeightKg: ex.targetWeightKg,
  }
}

function isDirty(ex) {
  const e = edits[ex.id]
  return e && (
    e.exerciseName !== ex.exerciseName ||
    e.targetSets !== ex.targetSets ||
    e.targetReps !== ex.targetReps ||
    e.targetWeightKg !== ex.targetWeightKg
  )
}

function reset(ex) { initEdit(ex) }

async function save(ex) {
  saving[ex.id] = true
  try {
    await updateExercise(routineId, ex.id, edits[ex.id])
    ex.exerciseName = edits[ex.id].exerciseName
    ex.targetSets = edits[ex.id].targetSets
    ex.targetReps = edits[ex.id].targetReps
    ex.targetWeightKg = edits[ex.id].targetWeightKg
    saved[ex.id] = true
    setTimeout(() => { saved[ex.id] = false }, 2000)
  } catch {
    initEdit(ex)
  } finally {
    saving[ex.id] = false
  }
}

async function removeExercise(ex) {
  deleting[ex.id] = true
  try {
    await deleteExercise(routineId, ex.id)
    routine.value.exercises = routine.value.exercises.filter(e => e.id !== ex.id)
  } finally {
    deleting[ex.id] = false
  }
}

function openAddForm(dayLabel) {
  addForm.dayLabel = dayLabel
  addForm.exerciseName = ''
  addForm.targetSets = 3
  addForm.targetReps = 10
  addForm.targetWeightKg = 0
}
function closeAddForm() { addForm.dayLabel = '' }

async function confirmAdd(day) {
  if (!addForm.exerciseName.trim()) return
  adding.value = true
  try {
    const newEx = await addExercise(routineId, {
      dayLabel: day.dayLabel,
      exerciseName: addForm.exerciseName.trim(),
      targetSets: addForm.targetSets,
      targetReps: addForm.targetReps,
      targetWeightKg: addForm.targetWeightKg,
    })
    routine.value.exercises.push(newEx)
    initEdit(newEx)
    closeAddForm()
  } finally {
    adding.value = false
  }
}

// ── 운동 모드 함수 ─────────────────────────────────
function openSplitSelect() {
  sessionDone.value = false
  splitSelectOpen.value = true
}

function startWorkoutForDay(dayLabel) {
  splitSelectOpen.value = false
  selectedDay.value = dayLabel

  const exercises = groupedDays.value.find(d => d.dayLabel === dayLabel)?.exercises ?? []
  for (const ex of exercises) {
    sessionSets[ex.id] = Array.from({ length: ex.targetSets }, (_, i) => ({
      setNumber: i + 1,
      actualReps: ex.targetReps,
      actualWeightKg: ex.targetWeightKg,
      completed: false,
    }))
  }
  caloriesBurned.value = 0
  isAutoCalc.value = true
  workoutMode.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function cancelWorkout() {
  workoutMode.value = false
  selectedDay.value = ''
}

async function finishWorkout() {
  const calories = Number(caloriesBurned.value) || 0
  submitting.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const sets = selectedExercises.value.flatMap(ex =>
      (sessionSets[ex.id] ?? []).map(s => ({
        exerciseId: ex.id,
        exerciseName: ex.exerciseName,
        setNumber: s.setNumber,
        actualReps: s.actualReps,
        actualWeightKg: s.actualWeightKg,
        completed: s.completed,
      }))
    )
    await recordSession(routineId, { sessionDate: today, caloriesBurned: calories, sets })
    doneCalories.value = calories
    workoutMode.value = false
    selectedDay.value = ''
    sessionDone.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } finally {
    submitting.value = false
  }
}

// ── 초기 로딩 ──────────────────────────────────────
onMounted(async () => {
  loading.value = true
  try {
    routine.value = await getRoutine(routineId)
    for (const ex of routine.value.exercises) {
      initEdit(ex)
    }
  } catch (e) {
    pageError.value = e?.data?.message ?? '루틴을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
