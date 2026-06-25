<template>
  <div class="max-w-4xl mx-auto w-full">

    <!-- Header -->
    <header class="flex justify-between items-center mb-8">
      <div>
        <h2 class="text-headline-lg text-on-background">식단 기록</h2>
        <p class="text-body-md text-on-surface-variant mt-0.5">하루 목표 칼로리를 채워보세요.</p>
      </div>
      <div class="flex items-center gap-2 neo-brutal-border rounded-xl px-4 py-3 bg-white">
        <button class="w-8 h-8 flex items-center justify-center hover:bg-surface rounded-lg transition-colors" @click="prevDay">
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <button class="text-label-lg font-bold min-w-[130px] text-center hover:text-primary transition-colors" @click="goToday">
          {{ displayDate }}
        </button>
        <button
          class="w-8 h-8 flex items-center justify-center rounded-lg transition-colors"
          :class="isToday ? 'opacity-30 cursor-not-allowed' : 'hover:bg-surface'"
          :disabled="isToday"
          @click="nextDay"
        >
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
      </div>
    </header>

    <!-- 로딩 -->
    <div v-if="state.loading" class="flex items-center gap-4 text-on-surface-variant mb-8">
      <span class="material-symbols-outlined animate-spin">progress_activity</span>
      <span class="text-body-md">식단 기록을 불러오는 중...</span>
    </div>

    <template v-else>
      <div class="space-y-6">

          <!-- 칼로리 요약 카드 -->
          <div class="bg-white neo-brutal-border rounded-xl p-7">
            <!-- 칼로리 헤더 -->
            <div class="flex items-end justify-between mb-6">
              <div>
                <p class="text-label-lg text-on-surface-variant mb-2">오늘의 칼로리</p>
                <div class="flex items-baseline gap-2">
                  <span class="text-numeral-xl text-on-background leading-none">{{ formatNumber(totalCalories) }}</span>
                  <span class="text-headline-md text-on-surface-variant">kcal</span>
                </div>
              </div>
              <div class="text-right">
                <p class="text-body-md text-on-surface-variant">목표</p>
                <p class="text-headline-lg font-bold text-on-background">{{ formatNumber(targetCalories) }} kcal</p>
              </div>
            </div>

            <!-- 칼로리 프로그레스 바 -->
            <div class="h-8 bg-surface neo-brutal-border rounded-xl overflow-hidden">
              <div class="h-full bg-primary transition-all duration-700" :style="{ width: calorieProgress + '%' }" />
            </div>
            <div class="flex justify-between mt-3 mb-8">
              <span class="text-body-md font-bold text-primary">{{ calorieProgress }}% 달성</span>
              <span class="text-body-md text-on-surface-variant">{{ formatNumber(remainingCalories) }} kcal 남음</span>
            </div>

            <!-- 영양소 -->
            <div class="grid grid-cols-3 gap-6 pt-6 border-t-[3px] border-on-background">
              <div v-for="macro in macros" :key="macro.key">
                <div class="flex justify-between items-baseline mb-3">
                  <span class="text-body-md font-bold text-on-background">{{ macro.label }}</span>
                  <span class="text-label-lg text-on-surface-variant">{{ formatNumber(macro.current) }}<span class="text-on-surface-variant/60">/{{ MACRO_TARGETS[macro.key] }}g</span></span>
                </div>
                <div class="h-5 bg-surface neo-brutal-border rounded-xl overflow-hidden">
                  <div class="h-full transition-all duration-700" :class="macro.colorClass" :style="{ width: macro.percent + '%' }" />
                </div>
                <p class="text-label-lg text-on-surface-variant mt-2">{{ macro.percent }}%</p>
              </div>
            </div>
          </div>

          <!-- 끼니 카드 목록 -->
          <div class="space-y-5">
            <div
              v-for="(meal, index) in state.meals"
              :key="meal.id"
              class="bg-surface neo-brutal-border rounded-xl overflow-hidden"
            >
              <!-- 끼니 헤더 -->
              <div class="flex items-center justify-between p-5 border-b-[3px] border-on-background">
                <div class="flex items-center gap-4">
                  <div class="w-12 h-12 bg-primary neo-brutal-border rounded-xl flex items-center justify-center flex-shrink-0">
                    <span class="font-bold text-on-primary text-lg">{{ index + 1 }}</span>
                  </div>
                  <div>
                    <h3 class="text-headline-md font-bold">{{ ordinal(index + 1) }} 끼니</h3>
                    <p class="text-label-lg text-on-surface-variant mt-0.5">
                      {{ mealCalories(meal) > 0 ? formatNumber(mealCalories(meal)) + ' kcal · ' + meal.items.length + '가지' : '음식을 추가해보세요' }}
                    </p>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    class="flex items-center gap-1.5 bg-white px-4 py-2 neo-brutal-border rounded-lg text-label-lg font-bold hover:-translate-y-0.5 transition-transform"
                    @click="toggleSearch(meal.id)"
                  >
                    <span class="material-symbols-outlined text-base">{{ search.mealId === meal.id ? 'close' : 'add' }}</span>
                    {{ search.mealId === meal.id ? '닫기' : '추가' }}
                  </button>
                  <button
                    class="w-9 h-9 flex items-center justify-center hover:bg-surface rounded-lg transition-colors text-on-surface-variant"
                    :disabled="state.deleting === meal.id"
                    @click="handleDelete(meal.id)"
                  >
                    <span class="material-symbols-outlined text-xl">{{ state.deleting === meal.id ? 'progress_activity' : 'delete' }}</span>
                  </button>
                </div>
              </div>

              <!-- 음식 목록 -->
              <div v-if="meal.items?.length > 0" class="divide-y-[3px] divide-on-background">
                <div v-for="item in meal.items" :key="item.foodCode" class="flex items-center justify-between px-5 py-4">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 bg-white neo-brutal-border rounded-lg flex items-center justify-center flex-shrink-0">
                      <span class="material-symbols-outlined text-on-surface-variant text-xl" style="font-variation-settings:'FILL' 1;">nutrition</span>
                    </div>
                    <div>
                      <p class="font-bold text-on-background">{{ item.foodName }}</p>
                      <p class="text-label-lg text-on-surface-variant mt-0.5">{{ formatNumber(item.amountGrams) }}g · 단백질 {{ formatNumber(item.protein) }}g</p>
                    </div>
                  </div>
                  <p class="font-bold text-primary flex-shrink-0">{{ formatNumber(item.calories) }} kcal</p>
                </div>
              </div>

              <!-- 인라인 식단 추가 패널 -->
              <div v-if="search.mealId === meal.id" class="border-t-[3px] border-on-background bg-white p-5 space-y-4">
                <div class="flex gap-3">
                  <input
                    v-model="search.query"
                    type="search"
                    placeholder="음식 이름 검색 (예: 닭가슴살)"
                    class="flex-1 px-4 py-3 neo-brutal-border rounded-xl text-body-md focus:outline-none focus:ring-2 focus:ring-primary bg-background"
                    @keydown.enter.prevent="handleSearch"
                  />
                  <button
                    class="px-5 py-3 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-transform disabled:opacity-50"
                    :disabled="search.loading"
                    @click="handleSearch"
                  >
                    <span v-if="search.loading" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
                    <span v-else class="material-symbols-outlined text-xl">search</span>
                  </button>
                </div>

                <div v-if="search.results.length > 0" class="neo-brutal-border rounded-xl overflow-hidden max-h-52 overflow-y-auto">
                  <button
                    v-for="food in search.results"
                    :key="food.foodCode"
                    class="w-full flex items-center justify-between px-4 py-3 border-b-[2px] border-on-background last:border-b-0 text-left transition-colors"
                    :class="search.selected?.foodCode === food.foodCode ? 'bg-primary text-on-primary' : 'bg-white hover:bg-surface'"
                    @click="selectFood(food)"
                  >
                    <span class="font-bold">{{ food.name }}</span>
                    <span
                      class="text-label-lg"
                      :class="search.selected?.foodCode === food.foodCode ? 'text-on-primary/70' : 'text-on-surface-variant'"
                    >100g당 {{ formatNumber(food.caloriesPer100g) }} kcal</span>
                  </button>
                </div>

                <div v-if="search.selected" class="flex items-center gap-3 p-4 bg-surface neo-brutal-border rounded-xl">
                  <div class="flex-1 min-w-0">
                    <p class="font-bold text-on-background truncate mb-1">{{ search.selected.name }}</p>
                    <p class="text-label-lg text-on-surface-variant">
                      {{ formatNumber(search.amount) }}g = 약 {{ formatNumber(search.selected.caloriesPer100g * search.amount / 100) }} kcal
                    </p>
                  </div>
                  <input
                    v-model.number="search.amount"
                    type="number"
                    min="1"
                    step="10"
                    class="w-24 px-3 py-2 neo-brutal-border rounded-xl text-center font-bold focus:outline-none focus:ring-2 focus:ring-primary bg-white"
                  />
                  <span class="text-body-md font-bold text-on-surface-variant">g</span>
                  <button
                    class="px-5 py-3 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-transform disabled:opacity-50 flex items-center gap-2"
                    :disabled="search.saving"
                    @click="confirmAdd(meal.id)"
                  >
                    <span v-if="search.saving" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
                    <span v-else class="material-symbols-outlined text-xl">check</span>
                    추가
                  </button>
                </div>
              </div>

              <div v-if="!meal.items?.length && search.mealId !== meal.id" class="px-5 py-8 text-center text-on-surface-variant text-body-md">
                '추가' 버튼으로 음식을 기록하세요.
              </div>
            </div>

            <div v-if="state.meals.length === 0" class="py-16 flex flex-col items-center gap-4 text-on-surface-variant">
              <div class="w-16 h-16 bg-surface neo-brutal-border rounded-xl flex items-center justify-center">
                <span class="material-symbols-outlined text-4xl opacity-30" style="font-variation-settings:'FILL' 1;">restaurant_menu</span>
              </div>
              <div class="text-center">
                <p class="text-body-md">아직 오늘 기록한 끼니가 없어요.</p>
                <p class="text-label-lg text-on-surface-variant opacity-60 mt-1">아래 버튼으로 첫 끼니를 추가해보세요.</p>
              </div>
            </div>
          </div>

          <!-- 끼니 추가 버튼 -->
          <button
            class="flex items-center justify-center gap-3 w-full py-5 neo-brutal-border rounded-xl text-label-lg font-bold transition-all duration-150"
            :class="state.meals.length >= MAX_MEALS
              ? 'bg-surface text-on-surface-variant cursor-not-allowed opacity-50'
              : 'bg-primary text-on-primary neo-brutal-shadow hover:-translate-y-0.5'"
            :disabled="state.meals.length >= MAX_MEALS"
            @click="addMeal"
          >
            <span class="material-symbols-outlined text-2xl" style="font-variation-settings:'FILL' 1;">
              {{ state.meals.length >= MAX_MEALS ? 'block' : 'add_circle' }}
            </span>
            {{ state.meals.length >= MAX_MEALS ? `하루 최대 ${MAX_MEALS}끼니` : '끼니 추가' }}
          </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'

import {
  addMealItem,
  deleteMeal,
  getCalorieBalance,
  getDailySummary,
  getLastMealRecommendation,
  listMeals,
  recordMeal,
  searchFoods,
} from '@/api/dashboard'
import { useBadgeStore } from '@/stores/badge'
import { getEffectiveToday } from '@/utils/effectiveDate'

const badgeStore = useBadgeStore()

const MACRO_TARGETS = { carbs: 250, protein: 120, fat: 60 }
const ORDINALS = ['첫', '두', '세', '네', '다섯', '여섯', '일곱', '여덟', '아홉', '열']

const currentDate = ref(getEffectiveToday())
const state = reactive({
  loading: true,
  summary: null,
  balance: null,
  meals: [],
  deleting: null,
})

const recommend = reactive({
  loading: false,
  error: '',
  response: null,
})

const search = reactive({
  mealId: null,
  query: '',
  results: [],
  loading: false,
  selected: null,
  amount: 100,
  saving: false,
})

const isToday = computed(() => formatDate(currentDate.value) === formatDate(getEffectiveToday()))

const displayDate = computed(() => {
  const d = currentDate.value
  const today = getEffectiveToday()
  if (formatDate(d) === formatDate(today)) return '오늘'
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (formatDate(d) === formatDate(yesterday)) return '어제'
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
})

const targetCalories = computed(() => state.balance?.targetCalories ?? state.summary?.targetCalories ?? 1800)
const totalCalories = computed(() => state.balance?.intakeCalories ?? state.summary?.achievedCalories ?? 0)
const remainingCalories = computed(() => Math.max(targetCalories.value - totalCalories.value, 0))
const calorieProgress = computed(() =>
  clamp(targetCalories.value ? Math.round((totalCalories.value / targetCalories.value) * 100) : 0)
)

const canRecommend = computed(() => Boolean(state.balance?.lastMealRecommendTrigger))

const coachMessage = computed(() => {
  if (state.balance?.lastMealRecommendTrigger) return '마지막 끼니에서 부족한 영양소를 채워볼까요?'
  if (calorieProgress.value >= 100) return '오늘 목표를 달성했어요! 훌륭해요.'
  return `${formatNumber(remainingCalories.value)} kcal 남았어요. 다음 끼니를 기록해볼까요?`
})

const primaryRecommendation = computed(() => {
  const rec = recommend.response?.recommendations?.[0]
  if (rec) return { name: rec.name, reason: rec.reason ?? '잔여 영양소 기반', kcal: rec.kcal ?? 0 }
  if (canRecommend.value) return { name: '추천을 요청해 주세요', reason: '아래 버튼을 눌러 AI 추천을 받아보세요.', kcal: remainingCalories.value }
  return null
})

const macros = computed(() => [
  { key: 'carbs',   label: '탄수화물', current: state.summary?.totalCarbs ?? 0,   colorClass: 'bg-carbs',   percent: clamp(Math.round(((state.summary?.totalCarbs ?? 0)   / MACRO_TARGETS.carbs)   * 100)) },
  { key: 'protein', label: '단백질',   current: state.summary?.totalProtein ?? 0, colorClass: 'bg-protein', percent: clamp(Math.round(((state.summary?.totalProtein ?? 0) / MACRO_TARGETS.protein) * 100)) },
  { key: 'fat',     label: '지방',     current: state.summary?.totalFat ?? 0,     colorClass: 'bg-fat',     percent: clamp(Math.round(((state.summary?.totalFat ?? 0)     / MACRO_TARGETS.fat)     * 100)) },
])

function mealCalories(meal) {
  return (meal.items ?? []).reduce((sum, item) => sum + (item.calories ?? 0), 0)
}

function ordinal(n) {
  return n <= ORDINALS.length ? ORDINALS[n - 1] : `${n}`
}

function toggleSearch(mealId) {
  if (search.mealId === mealId) {
    closeSearch()
  } else {
    search.mealId = mealId
    search.query = ''
    search.results = []
    search.selected = null
    search.amount = 100
  }
}

function closeSearch() {
  search.mealId = null
  search.query = ''
  search.results = []
  search.selected = null
}

function selectFood(food) {
  search.selected = food
  search.amount = 100
}

async function handleSearch() {
  if (!search.query.trim()) return
  search.loading = true
  search.results = []
  search.selected = null
  try {
    search.results = await searchFoods(search.query.trim())
  } finally {
    search.loading = false
  }
}

async function confirmAdd(mealId) {
  if (!search.selected || !search.amount) return
  search.saving = true
  try {
    const meal = state.meals.find((m) => m.id === mealId)
    let updated

    if (meal?._isTemp) {
      updated = await recordMeal({
        type: inferMealType(),
        date: formatDate(currentDate.value),
        foodCode: search.selected.foodCode,
        amountGrams: search.amount,
      })
      badgeStore.celebrate(updated)
      const idx = state.meals.findIndex((m) => m.id === mealId)
      if (idx !== -1) state.meals[idx] = updated
      search.mealId = updated.id
    } else {
      updated = await addMealItem(mealId, {
        foodCode: search.selected.foodCode,
        amountGrams: search.amount,
      })
      badgeStore.celebrate(updated)
      const idx = state.meals.findIndex((m) => m.id === mealId)
      if (idx !== -1) state.meals[idx] = updated
    }

    search.selected = null
    search.query = ''
    search.results = []
    search.amount = 100
    refreshSummary()
  } catch {
    if (state.meals.find((m) => m.id === mealId)?._isTemp) {
      state.meals = state.meals.filter((m) => m.id !== mealId)
      closeSearch()
    }
  } finally {
    search.saving = false
  }
}

const MAX_MEALS = 8

function addMeal() {
  if (state.meals.length >= MAX_MEALS) return
  const tempId = -Date.now()
  state.meals.push({ id: tempId, items: [], _isTemp: true })
  toggleSearch(tempId)
}

async function handleDelete(mealId) {
  if (search.mealId === mealId) closeSearch()
  const meal = state.meals.find((m) => m.id === mealId)
  if (meal?._isTemp) {
    state.meals = state.meals.filter((m) => m.id !== mealId)
    return
  }
  state.deleting = mealId
  try {
    await deleteMeal(mealId)
    state.meals = state.meals.filter((m) => m.id !== mealId)
    refreshSummary()
  } finally {
    state.deleting = null
  }
}

async function loadRecommendation() {
  recommend.loading = true
  recommend.error = ''
  try {
    recommend.response = await getLastMealRecommendation()
  } catch {
    recommend.error = '추천을 불러오지 못했어요.'
  } finally {
    recommend.loading = false
  }
}

let loadEpoch = 0

async function loadData() {
  const epoch = ++loadEpoch
  state.loading = true
  const date = formatDate(currentDate.value)
  const [summary, balance, meals] = await Promise.allSettled([
    getDailySummary(date),
    getCalorieBalance(date),
    listMeals(date),
  ])
  if (epoch !== loadEpoch) return
  state.summary = summary.status === 'fulfilled' ? summary.value : null
  state.balance = balance.status === 'fulfilled' ? balance.value : null
  state.meals   = meals.status === 'fulfilled' ? meals.value : []
  state.loading = false

  if (canRecommend.value) await loadRecommendation()
}

async function refreshSummary() {
  const date = formatDate(currentDate.value)
  const [summary, balance] = await Promise.allSettled([
    getDailySummary(date),
    getCalorieBalance(date),
  ])
  if (summary.status === 'fulfilled') state.summary = summary.value
  if (balance.status === 'fulfilled') state.balance = balance.value
}

function prevDay() {
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() - 1)
  currentDate.value = d
}

function nextDay() {
  if (isToday.value) return
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() + 1)
  currentDate.value = d
}

function goToday() { currentDate.value = getEffectiveToday() }

function clamp(v) { return Math.max(0, Math.min(100, v)) }

function inferMealType() {
  const h = new Date().getHours()
  if (h >= 4 && h < 10) return 'BREAKFAST'
  if (h < 15) return 'LUNCH'
  if (h < 20) return 'DINNER'
  return 'SNACK'
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

function formatKcal(v) { return `${formatNumber(v)} kcal` }

watch(currentDate, loadData, { immediate: true })
</script>
