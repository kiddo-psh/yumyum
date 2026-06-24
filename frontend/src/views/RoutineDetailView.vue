<template>
  <header class="mb-8">
    <RouterLink
      to="/routine"
      class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
    >
      <span class="material-symbols-outlined text-base">chevron_left</span>
      <span>운동 루틴으로</span>
    </RouterLink>
    <div v-if="routine" class="flex items-center justify-between gap-4">
      <div>
        <h1 class="text-display-md text-on-background">{{ routine.name }}</h1>
        <p class="text-body-md text-on-surface-variant mt-1">주 {{ routine.daysPerWeek }}회</p>
      </div>
      <span
        class="shrink-0 neo-brutal-border rounded-full px-3 py-1.5 text-label-lg"
        :class="routine.aiGenerated ? 'bg-nyam-mint text-on-background' : 'bg-surface text-on-surface-variant'"
      >
        {{ routine.aiGenerated ? 'AI 생성' : '직접 등록' }}
      </span>
    </div>
  </header>

  <div v-if="loading" class="flex items-center gap-3 text-on-surface-variant py-6">
    <span class="material-symbols-outlined animate-spin">progress_activity</span>
    <span>루틴을 불러오는 중...</span>
  </div>

  <div v-else-if="error" class="bg-white neo-brutal-border rounded-xl p-6 text-danger text-body-md font-bold">
    {{ error }}
  </div>

  <template v-else-if="routine">
    <div v-if="routine.aiComment" class="bg-nyam-mint/20 neo-brutal-border rounded-xl p-5 mb-8">
      <p class="text-body-md text-on-background leading-relaxed">{{ routine.aiComment }}</p>
    </div>

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
          <!-- 운동 이름 -->
          <div class="flex items-center gap-2 mb-4">
            <input
              v-model="edits[ex.id].exerciseName"
              type="text"
              placeholder="운동 이름"
              class="flex-1 neo-brutal-border rounded-lg px-3 py-2 text-body-lg font-bold text-on-background bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
            />
            <button
              class="shrink-0 text-on-surface-variant hover:text-danger transition-colors"
              :disabled="deleting[ex.id]"
              @click="removeExercise(ex, day.dayLabel)"
              title="운동 삭제"
            >
              <span v-if="deleting[ex.id]" class="material-symbols-outlined text-lg animate-spin">progress_activity</span>
              <span v-else class="material-symbols-outlined text-lg">delete</span>
            </button>
          </div>

          <!-- 세트 / 횟수 / 무게 -->
          <div class="grid grid-cols-3 gap-3">
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">세트</p>
              <input
                v-model.number="edits[ex.id].targetSets"
                type="number" min="1" step="1"
                class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">횟수</p>
              <input
                v-model.number="edits[ex.id].targetReps"
                type="number" min="1" step="1"
                class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">무게 (kg)</p>
              <input
                v-model.number="edits[ex.id].targetWeightKg"
                type="number" min="0" step="0.5"
                class="w-full neo-brutal-border rounded-lg px-3 py-2 text-body-md text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>

          <!-- 저장 / 취소 (변경 시만) -->
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
            <button
              class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors"
              @click="reset(ex)"
            >
              취소
            </button>
          </div>
          <p v-if="saved[ex.id]" class="flex items-center gap-1 text-success text-label-sm mt-2">
            <span class="material-symbols-outlined text-sm" style="font-variation-settings:'FILL' 1;">check_circle</span>
            저장됨
          </p>
        </li>
      </ul>

      <!-- 운동 추가 버튼 -->
      <button
        class="w-full flex items-center justify-center gap-2 border-2 border-dashed border-outline rounded-xl py-3 text-label-lg text-on-surface-variant hover:border-primary hover:text-primary transition-colors"
        @click="openAddForm(day.dayLabel)"
      >
        <span class="material-symbols-outlined text-lg">add</span>
        {{ day.dayLabel }} 운동 추가
      </button>

      <!-- 추가 폼 (해당 분할만 열림) -->
      <div
        v-if="addForm.dayLabel === day.dayLabel"
        class="mt-3 bg-white neo-brutal-border rounded-xl p-5"
      >
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
          <button
            class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors"
            @click="closeAddForm"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  </template>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getRoutine, updateExercise, addExercise, deleteExercise } from '@/api/routine'

const route = useRoute()
const routineId = Number(route.params.routineId)

const routine = ref(null)
const loading = ref(false)
const error = ref('')
const edits = reactive({})
const saving = reactive({})
const saved = reactive({})
const deleting = reactive({})
const adding = ref(false)

const addForm = reactive({
  dayLabel: '',
  exerciseName: '',
  targetSets: 3,
  targetReps: 10,
  targetWeightKg: 0,
})

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

function reset(ex) {
  initEdit(ex)
}

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

async function removeExercise(ex, dayLabel) {
  deleting[ex.id] = true
  try {
    await deleteExercise(routineId, ex.id)
    const day = groupedDays.value.find(d => d.dayLabel === dayLabel)
    if (day) {
      const idx = day.exercises.indexOf(ex)
      if (idx !== -1) day.exercises.splice(idx, 1)
    }
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

function closeAddForm() {
  addForm.dayLabel = ''
}

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

onMounted(async () => {
  loading.value = true
  try {
    routine.value = await getRoutine(routineId)
    for (const ex of routine.value.exercises) {
      initEdit(ex)
    }
  } catch (e) {
    error.value = e?.data?.message ?? '루틴을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
})
</script>
