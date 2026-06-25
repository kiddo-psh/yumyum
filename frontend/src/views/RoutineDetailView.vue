<template>
  <div class="max-w-4xl mx-auto w-full">

    <!-- 헤더 -->
    <header class="mb-8">
      <RouterLink
        to="/routine"
        class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-4"
      >
        <span class="material-symbols-outlined text-base">chevron_left</span>
        <span>운동 루틴으로</span>
      </RouterLink>

      <div v-if="routine" class="flex items-start justify-between gap-4">
        <div>
          <h1 class="text-display-md text-on-background">{{ routine.name }}</h1>
          <div class="flex items-center gap-3 mt-2">
            <p class="text-body-md text-on-surface-variant">주 {{ routine.daysPerWeek }}회</p>
            <span
              class="neo-brutal-border rounded-full px-3 py-1 text-label-lg"
              :class="routine.aiGenerated ? 'bg-on-background text-white' : 'bg-surface text-on-surface-variant'"
            >{{ routine.aiGenerated ? 'AI 생성' : '직접 등록' }}</span>
          </div>
        </div>

        <div class="shrink-0">
          <template v-if="!workoutMode">
            <button
              class="flex items-center gap-2 bg-primary text-on-primary neo-brutal-border neo-brutal-shadow rounded-xl px-5 py-3 text-label-lg font-bold hover:-translate-y-0.5 active:translate-y-1 transition-all duration-150"
              @click="openSplitSelect"
            >
              <span class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">play_circle</span>
              운동 시작
            </button>
          </template>
          <template v-else>
            <div class="flex items-center gap-2">
              <button
                class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors px-3 py-2"
                @click="cancelWorkout"
              >취소</button>
              <button
                class="flex items-center gap-2 bg-success text-white neo-brutal-border rounded-xl px-5 py-3 text-label-lg font-bold hover:-translate-y-0.5 transition-transform disabled:opacity-40"
                :disabled="submitting"
                @click="finishWorkout"
              >
                <span v-if="submitting" class="material-symbols-outlined animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">check_circle</span>
                완료
              </button>
            </div>
          </template>
        </div>
      </div>
    </header>

    <!-- 로딩 / 에러 -->
    <div v-if="loading" class="flex items-center gap-3 text-on-surface-variant py-10">
      <span class="material-symbols-outlined animate-spin">progress_activity</span>
      <span class="text-body-md">루틴을 불러오는 중...</span>
    </div>
    <div v-else-if="pageError" class="bg-white neo-brutal-border rounded-xl p-6 text-danger text-body-md font-bold">
      {{ pageError }}
    </div>

    <!-- 완료 배너 -->
    <div v-if="sessionDone" class="bg-success neo-brutal-border rounded-xl p-6 mb-8">
      <div class="flex items-center gap-4 mb-4">
        <div class="w-14 h-14 bg-white neo-brutal-border rounded-xl flex items-center justify-center shrink-0">
          <span class="material-symbols-outlined text-success text-3xl" style="font-variation-settings:'FILL' 1;">emoji_events</span>
        </div>
        <div>
          <p class="text-headline-md font-bold text-white">운동 완료!</p>
          <p class="text-label-lg text-white/80 mt-0.5">AI가 다음 주 루틴을 자동으로 조정합니다.</p>
        </div>
      </div>
      <div class="flex items-center justify-between bg-black/10 rounded-xl px-5 py-3">
        <div class="flex items-center gap-2">
          <span class="material-symbols-outlined text-white" style="font-variation-settings:'FILL' 1;">local_fire_department</span>
          <span class="text-label-lg text-white/90 font-bold">소모 칼로리</span>
        </div>
        <span class="text-headline-md font-bold text-white">{{ doneCalories }} kcal</span>
      </div>
    </div>

    <template v-if="routine && !loading && !pageError">

      <!-- AI 코멘트 -->
      <div v-if="routine.aiComment && !workoutMode" class="bg-white neo-brutal-border rounded-xl p-5 mb-8">
        <div class="flex items-start gap-3">
          <img src="/nyam/nyam_coach.png" alt="냠냠코치" class="w-10 h-10 object-contain shrink-0 mt-0.5" />
          <p class="text-body-md text-on-background leading-relaxed">{{ routine.aiComment }}</p>
        </div>
      </div>

      <!-- ── 분할 선택 모달 ── -->
      <div
        v-if="splitSelectOpen"
        class="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50"
        @click.self="splitSelectOpen = false"
      >
        <div class="w-full sm:max-w-md bg-background neo-brutal-border rounded-t-2xl sm:rounded-2xl p-6">
          <p class="text-headline-md text-on-background font-bold mb-1">오늘 어떤 운동할까요?</p>
          <p class="text-label-lg text-on-surface-variant mb-5">분할을 선택하면 운동이 시작됩니다.</p>
          <ul class="grid gap-3">
            <li v-for="day in uniqueDays" :key="day">
              <button
                class="w-full flex items-center justify-between neo-brutal-border rounded-xl px-5 py-4 bg-white hover:bg-surface hover:-translate-y-0.5 transition-all duration-150"
                @click="startWorkoutForDay(day)"
              >
                <span class="text-body-lg font-bold text-on-background">{{ day }}</span>
                <div class="flex items-center gap-2 text-on-surface-variant">
                  <span class="text-label-lg">{{ exerciseCountForDay(day) }}가지 운동</span>
                  <span class="material-symbols-outlined text-base">chevron_right</span>
                </div>
              </button>
            </li>
          </ul>
          <button
            class="mt-4 w-full text-label-lg text-on-surface-variant hover:text-on-background py-3 transition-colors"
            @click="splitSelectOpen = false"
          >취소</button>
        </div>
      </div>

      <!-- ── 운동 모드 ── -->
      <template v-if="workoutMode">

        <!-- 진행률 카드 -->
        <div class="bg-white neo-brutal-border rounded-xl p-6 mb-6">
          <div class="flex items-center justify-between mb-5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">fitness_center</span>
              <h2 class="text-headline-md font-bold text-on-background">{{ selectedDay }}</h2>
            </div>
            <p class="text-label-lg text-on-surface-variant">
              <span class="text-headline-md font-bold text-on-background">{{ completedSetCount }}</span>
              &nbsp;/&nbsp;{{ totalSetCount }} 세트
            </p>
          </div>

          <div class="h-8 bg-surface neo-brutal-border rounded-xl overflow-hidden">
            <div
              class="h-full bg-primary transition-all duration-700"
              :style="{ width: totalSetCount ? `${Math.round(completedSetCount / totalSetCount * 100)}%` : '0%' }"
            />
          </div>

          <div class="flex items-center justify-between mt-3">
            <span class="text-label-lg text-on-surface-variant">세트 완료</span>
            <span class="text-headline-md font-bold text-primary">{{ totalSetCount ? Math.round(completedSetCount / totalSetCount * 100) : 0 }}%</span>
          </div>
        </div>

        <!-- 소모 칼로리 -->
        <div class="bg-white neo-brutal-border rounded-xl p-5 mb-6">
          <div class="flex items-center gap-2 mb-3">
            <span class="material-symbols-outlined text-primary" style="font-variation-settings:'FILL' 1;">local_fire_department</span>
            <p class="text-label-lg font-bold text-on-surface-variant">소모 칼로리</p>
            <span v-if="isAutoCalc" class="ml-auto text-xs text-on-surface-variant bg-surface neo-brutal-border rounded-full px-2.5 py-0.5">자동 계산</span>
          </div>
          <div class="flex items-center gap-3">
            <input
              v-model.number="caloriesBurned"
              type="number" min="0" step="10"
              class="flex-1 neo-brutal-border rounded-xl px-4 py-3 text-headline-lg font-bold text-center bg-surface focus:outline-none focus:ring-2 focus:ring-primary min-w-0"
              @input="isAutoCalc = false"
            />
            <span class="text-body-lg font-bold text-on-surface-variant shrink-0">kcal</span>
          </div>
          <p v-if="!isAutoCalc" class="text-xs text-on-surface-variant mt-2">
            예상치 {{ estimatedCalories }} kcal ·
            <button class="text-primary hover:underline" @click="isAutoCalc = true; caloriesBurned = estimatedCalories">자동으로 되돌리기</button>
          </p>
          <p v-else class="text-xs text-on-surface-variant mt-2">세트를 완료하면 자동으로 계산됩니다</p>
        </div>

        <!-- 운동 카드 목록 -->
        <ul class="grid gap-5">
          <li
            v-for="(ex, exIdx) in selectedExercises"
            :key="ex.id"
            class="bg-white neo-brutal-border rounded-xl overflow-hidden"
          >
            <!-- 운동 헤더 -->
            <div class="flex items-center gap-4 px-5 py-4 bg-surface border-b-[3px] border-on-background">
              <div class="w-9 h-9 bg-on-background rounded-xl flex items-center justify-center shrink-0">
                <span class="text-white font-bold text-label-lg">{{ exIdx + 1 }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-headline-md font-bold text-on-background truncate">{{ ex.exerciseName }}</p>
                <p class="text-xs text-on-surface-variant mt-0.5">목표 {{ ex.targetSets }}세트 × {{ ex.targetReps }}회 · {{ ex.targetWeightKg }}kg</p>
              </div>
              <div class="shrink-0 text-right">
                <p class="text-label-lg font-bold text-primary">
                  {{ (sessionSets[ex.id] ?? []).filter(s => s.completed).length }} / {{ ex.targetSets }}
                </p>
              </div>
            </div>

            <!-- 세트 목록 -->
            <div class="divide-y divide-outline">
              <div
                v-for="s in sessionSets[ex.id]"
                :key="s.setNumber"
                class="flex items-center gap-4 px-5 py-4"
              >
                <!-- 완료 체크 버튼 -->
                <button
                  class="w-11 h-11 rounded-2xl neo-brutal-border flex items-center justify-center shrink-0 transition-all duration-150"
                  :class="s.completed ? 'bg-primary -translate-y-0.5' : 'bg-surface hover:bg-outline'"
                  @click="s.completed = !s.completed"
                >
                  <span
                    class="material-symbols-outlined"
                    :style="{ 'font-variation-settings': `'FILL' ${s.completed ? 1 : 0}` }"
                    :class="s.completed ? 'text-on-primary' : 'text-on-surface-variant'"
                  >check</span>
                </button>

                <!-- 세트 번호 -->
                <span
                  class="text-label-lg font-bold shrink-0 w-10 transition-colors"
                  :class="s.completed ? 'text-primary' : 'text-on-surface-variant'"
                >{{ s.setNumber }}세트</span>

                <!-- 횟수 -->
                <div class="flex items-baseline justify-center gap-1 flex-1">
                  <input
                    v-model.number="s.actualReps"
                    type="number" min="0" step="1"
                    class="w-14 text-headline-md font-bold text-center bg-transparent focus:outline-none focus:ring-2 focus:ring-primary focus:bg-surface focus:rounded-xl transition-colors"
                    :class="s.completed ? 'text-on-surface-variant' : 'text-on-background'"
                  />
                  <span class="text-body-md font-bold text-on-surface-variant shrink-0">회</span>
                </div>

                <span class="text-body-md font-bold text-on-surface-variant shrink-0">×</span>

                <!-- 무게 -->
                <div class="flex items-baseline justify-center gap-1 flex-1">
                  <input
                    v-model.number="s.actualWeightKg"
                    type="number" min="0" step="0.5"
                    class="w-14 text-headline-md font-bold text-center bg-transparent focus:outline-none focus:ring-2 focus:ring-primary focus:bg-surface focus:rounded-xl transition-colors"
                    :class="s.completed ? 'text-on-surface-variant' : 'text-on-background'"
                  />
                  <span class="text-body-md font-bold text-on-surface-variant shrink-0">kg</span>
                </div>
              </div>
            </div>
          </li>
        </ul>

        <!-- 완료 버튼 (sticky) -->
        <div class="sticky bottom-0 z-10 flex justify-center pt-12 pb-6 bg-gradient-to-t from-background via-background/80 to-transparent pointer-events-none mt-4">
          <button
            class="pointer-events-auto flex items-center gap-3 bg-success text-white neo-brutal-border neo-brutal-shadow rounded-2xl px-10 py-4 text-headline-md font-bold hover:-translate-y-0.5 active:translate-y-1 transition-all duration-150 disabled:opacity-40"
            :disabled="submitting"
            @click="finishWorkout"
          >
            <span v-if="submitting" class="material-symbols-outlined animate-spin">progress_activity</span>
            <span v-else class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">check_circle</span>
            운동 완료
          </button>
        </div>
      </template>

      <!-- ── 편집 모드 ── -->
      <template v-else>
        <div v-for="(day, dayIdx) in groupedDays" :key="day.dayLabel" class="mb-10">

          <!-- 날 헤더 (토글) -->
          <button
            class="w-full flex items-center gap-5 px-2 py-3 mb-5 text-left"
            @click="toggleDay(day.dayLabel)"
          >
            <div class="w-14 h-14 bg-primary neo-brutal-border rounded-xl flex items-center justify-center shrink-0">
              <span class="text-on-primary font-bold text-headline-md">{{ dayIdx + 1 }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <h2 class="text-headline-lg font-bold text-on-background">{{ day.dayLabel }}</h2>
              <p class="text-body-md text-on-surface-variant mt-0.5">{{ day.exercises.length }}가지 운동</p>
            </div>
            <div
              class="shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-xl neo-brutal-border font-bold transition-all duration-150"
              :class="collapsedDays[day.dayLabel] ? 'bg-surface text-on-surface-variant' : 'bg-on-background text-white'"
            >
              <span
                class="material-symbols-outlined text-lg leading-none transition-transform duration-200"
                :class="collapsedDays[day.dayLabel] ? '' : 'rotate-180'"
              >expand_more</span>
              <span class="text-label-lg">{{ collapsedDays[day.dayLabel] ? '펼치기' : '접기' }}</span>
            </div>
          </button>

          <template v-if="!collapsedDays[day.dayLabel]">

          <!-- 운동 카드 -->
          <ul class="grid gap-4 mb-4">
            <li
              v-for="(ex, exIdx) in day.exercises"
              :key="ex.id"
              class="bg-white neo-brutal-border rounded-xl overflow-hidden"
            >
              <!-- 운동 이름 (인라인 편집) -->
              <div class="flex items-center gap-3 px-5 py-4">
                <span class="shrink-0 w-6 h-6 rounded-lg bg-surface flex items-center justify-center text-xs font-bold text-on-surface-variant">{{ exIdx + 1 }}</span>
                <input
                  v-model="edits[ex.id].exerciseName"
                  type="text"
                  placeholder="운동 이름"
                  class="flex-1 text-headline-md font-bold text-on-background bg-transparent focus:outline-none placeholder:text-on-surface-variant/40 min-w-0"
                />
                <button
                  class="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center text-on-surface-variant hover:text-danger hover:bg-surface transition-colors disabled:opacity-40"
                  :disabled="deleting[ex.id]"
                  @click="removeExercise(ex)"
                >
                  <span v-if="deleting[ex.id]" class="material-symbols-outlined text-base animate-spin">progress_activity</span>
                  <span v-else class="material-symbols-outlined text-base">delete</span>
                </button>
              </div>

              <!-- 세트 / 횟수 / 무게 -->
              <div class="flex gap-2.5 px-5 pb-5">
                <div class="flex-1 bg-surface rounded-xl py-3 px-2 text-center">
                  <input
                    v-model.number="edits[ex.id].targetSets"
                    type="number" min="1" step="1"
                    class="w-full text-headline-lg font-bold text-center bg-transparent focus:outline-none"
                  />
                  <p class="text-xs text-on-surface-variant mt-1">세트</p>
                </div>
                <div class="flex-1 bg-surface rounded-xl py-3 px-2 text-center">
                  <input
                    v-model.number="edits[ex.id].targetReps"
                    type="number" min="1" step="1"
                    class="w-full text-headline-lg font-bold text-center bg-transparent focus:outline-none"
                  />
                  <p class="text-xs text-on-surface-variant mt-1">횟수</p>
                </div>
                <div class="flex-1 bg-surface rounded-xl py-3 px-2 text-center">
                  <input
                    v-model.number="edits[ex.id].targetWeightKg"
                    type="number" min="0" step="0.5"
                    class="w-full text-headline-lg font-bold text-center bg-transparent focus:outline-none"
                  />
                  <p class="text-xs text-on-surface-variant mt-1">무게 kg</p>
                </div>
              </div>

              <!-- 저장/취소 (변경 시만 표시) -->
              <div v-if="isDirty(ex)" class="flex items-center gap-3 px-5 py-3 border-t-[3px] border-on-background">
                <button
                  class="flex items-center gap-1.5 bg-primary text-on-primary neo-brutal-border rounded-xl px-5 py-2.5 text-label-lg font-bold disabled:opacity-40 hover:-translate-y-0.5 transition-transform"
                  :disabled="saving[ex.id]"
                  @click="save(ex)"
                >
                  <span v-if="saving[ex.id]" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                  <span v-else class="material-symbols-outlined text-sm">save</span>
                  저장
                </button>
                <button
                  class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors"
                  @click="reset(ex)"
                >취소</button>
              </div>

              <div v-else-if="saved[ex.id]" class="flex items-center gap-1.5 px-5 py-3 text-success text-xs border-t-[3px] border-on-background">
                <span class="material-symbols-outlined text-sm" style="font-variation-settings:'FILL' 1;">check_circle</span>
                저장됨
              </div>
            </li>
          </ul>

          <!-- 운동 추가 버튼 -->
          <button
            class="w-full flex items-center justify-center gap-2 border-[3px] border-dashed border-outline rounded-xl py-4 text-label-lg text-on-surface-variant hover:border-primary hover:text-primary transition-colors"
            @click="openAddForm(day.dayLabel)"
          >
            <span class="material-symbols-outlined text-lg">add_circle</span>
            {{ day.dayLabel }} 운동 추가
          </button>

          <!-- 운동 추가 폼 -->
          <div v-if="addForm.dayLabel === day.dayLabel" class="mt-3 bg-white neo-brutal-border rounded-xl overflow-hidden">
            <div class="px-5 py-4 bg-surface border-b-[3px] border-on-background">
              <p class="text-label-lg font-bold text-on-background">새 운동 추가</p>
            </div>
            <div class="p-5 space-y-4">
              <input
                v-model="addForm.exerciseName"
                type="text"
                placeholder="운동 이름 (예: 스쿼트)"
                class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md font-bold text-on-background bg-surface focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <div class="grid grid-cols-3 gap-3">
                <div class="text-center">
                  <p class="text-xs font-bold text-on-surface-variant mb-2">세트</p>
                  <input
                    v-model.number="addForm.targetSets"
                    type="number" min="1" step="1"
                    class="w-full neo-brutal-border rounded-xl px-2 py-3 text-headline-lg font-bold text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div class="text-center">
                  <p class="text-xs font-bold text-on-surface-variant mb-2">횟수</p>
                  <input
                    v-model.number="addForm.targetReps"
                    type="number" min="1" step="1"
                    class="w-full neo-brutal-border rounded-xl px-2 py-3 text-headline-lg font-bold text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div class="text-center">
                  <p class="text-xs font-bold text-on-surface-variant mb-2">무게 kg</p>
                  <input
                    v-model.number="addForm.targetWeightKg"
                    type="number" min="0" step="0.5"
                    class="w-full neo-brutal-border rounded-xl px-2 py-3 text-headline-lg font-bold text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
              <div class="flex gap-3 pt-1">
                <button
                  class="flex items-center gap-1.5 bg-primary text-on-primary neo-brutal-border rounded-xl px-6 py-3 text-label-lg font-bold disabled:opacity-40 hover:-translate-y-0.5 transition-transform"
                  :disabled="!addForm.exerciseName.trim() || adding"
                  @click="confirmAdd(day)"
                >
                  <span v-if="adding" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                  <span v-else class="material-symbols-outlined text-sm">add</span>
                  추가
                </button>
                <button
                  class="text-label-lg text-on-surface-variant hover:text-on-background transition-colors"
                  @click="closeAddForm"
                >취소</button>
              </div>
            </div>
          </div>

          </template>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getRoutine, updateExercise, addExercise, deleteExercise, recordSession } from '@/api/routine'
import { useBadgeStore } from '@/stores/badge'

const badgeStore = useBadgeStore()

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
const collapsedDays = reactive({})

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

function toggleDay(dayLabel) {
  collapsedDays[dayLabel] = !collapsedDays[dayLabel]
  if (collapsedDays[dayLabel] && addForm.dayLabel === dayLabel) closeAddForm()
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
    const sessionResult = await recordSession(routineId, { sessionDate: today, caloriesBurned: calories, sets })
    badgeStore.celebrate(sessionResult)
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
