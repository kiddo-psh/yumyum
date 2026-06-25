<template>
  <div class="max-w-2xl mx-auto w-full">

    <!-- Header -->
    <header class="mb-8">
      <RouterLink
        :to="mealId ? '/log' : '/'"
        class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
      >
        <span class="material-symbols-outlined text-base">chevron_left</span>
        {{ mealId ? '식단 기록으로' : '홈으로' }}
      </RouterLink>
      <h1 class="text-display-md text-on-background">
        {{ mealId ? '음식 추가' : isManualMode ? '식단 기록' : '음식 검색' }}
      </h1>
      <p class="text-body-md text-on-surface-variant mt-1">
        {{ mealId ? '기존 끼니에 음식을 추가합니다.' : isManualMode ? '음식을 검색하고 섭취량을 입력하세요.' : '음식 영양 정보를 조회합니다.' }}
      </p>
    </header>

    <!-- 검색 -->
    <div class="bg-surface neo-brutal-border rounded-xl p-6 mb-6">
      <form class="flex gap-3" @submit.prevent="handleSearch">
        <input
          id="food-query"
          v-model.trim="query"
          type="search"
          placeholder="음식 이름 검색 (예: 닭가슴살)"
          class="flex-1 px-4 py-3 neo-brutal-border rounded-xl text-body-md focus:outline-none focus:ring-2 focus:ring-primary bg-white"
        />
        <button
          type="submit"
          :disabled="searchState.loading"
          class="px-5 py-3 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-transform disabled:opacity-50 flex items-center gap-1"
        >
          <span v-if="searchState.loading" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
          <span v-else class="material-symbols-outlined text-xl">search</span>
          {{ searchState.loading ? '검색 중' : '검색' }}
        </button>
      </form>

      <p v-if="searchState.error" class="text-danger text-label-lg font-bold mt-3 p-3 bg-white neo-brutal-border rounded-xl">
        {{ searchState.error }}
      </p>
    </div>

    <!-- 검색 결과 -->
    <template v-if="searchState.success">
      <div v-if="foods.length > 0" class="bg-surface neo-brutal-border rounded-xl overflow-hidden mb-6">
        <p class="px-5 py-3 border-b-[3px] border-on-background text-label-lg font-bold text-on-surface-variant">
          {{ foods.length }}개 결과
        </p>
        <div class="divide-y-[3px] divide-on-background max-h-80 overflow-y-auto">
          <button
            v-for="food in foods"
            :key="food.id"
            type="button"
            class="w-full flex items-center justify-between px-5 py-4 text-left transition-colors"
            :class="selectedFood?.id === food.id ? 'bg-primary text-on-primary' : 'bg-white hover:bg-surface'"
            @click="selectFood(food)"
          >
            <div>
              <p class="font-bold">{{ food.name }}</p>
              <p
                class="text-label-lg mt-0.5"
                :class="selectedFood?.id === food.id ? 'text-on-primary/70' : 'text-on-surface-variant'"
              >
                100g당 {{ formatNumber(food.caloriesPer100g) }} kcal · 단백질 {{ formatNumber(food.proteinPer100g) }}g
              </p>
            </div>
            <span
              v-if="selectedFood?.id === food.id"
              class="material-symbols-outlined text-on-primary ml-3 shrink-0"
              style="font-variation-settings:'FILL' 1;"
            >check_circle</span>
          </button>
        </div>
      </div>

      <div v-else class="bg-surface neo-brutal-border rounded-xl p-10 text-center text-on-surface-variant mb-6">
        <span class="material-symbols-outlined text-4xl opacity-30 block mb-2">search_off</span>
        <p class="text-body-md">검색 결과가 없어요.</p>
        <p class="text-label-lg opacity-60 mt-1">다른 이름으로 검색해 보세요.</p>
      </div>
    </template>

    <!-- 기록 폼 (manual / mealId 모드) -->
    <div v-if="(isManualMode || mealId) && selectedFood" class="bg-white neo-brutal-border rounded-xl p-6">
      <!-- 선택된 음식 요약 -->
      <div class="flex items-center gap-3 mb-5 p-4 bg-surface neo-brutal-border rounded-xl">
        <div class="w-10 h-10 bg-primary neo-brutal-border rounded-xl flex items-center justify-center shrink-0">
          <span class="material-symbols-outlined text-on-primary" style="font-variation-settings:'FILL' 1;">nutrition</span>
        </div>
        <div>
          <p class="font-bold text-on-background">{{ selectedFood.name }}</p>
          <p class="text-label-lg text-on-surface-variant">100g당 {{ formatNumber(selectedFood.caloriesPer100g) }} kcal</p>
        </div>
      </div>

      <form class="space-y-5" @submit.prevent="handleRecord">
        <div v-if="!mealId">
          <label class="text-label-lg font-bold text-on-background mb-2 block">끼니 타입</label>
          <select
            v-model="recordForm.type"
            class="w-full px-4 py-3 neo-brutal-border rounded-xl text-body-md bg-white focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="BREAKFAST">아침</option>
            <option value="LUNCH">점심</option>
            <option value="DINNER">저녁</option>
            <option value="SNACK">간식</option>
          </select>
        </div>

        <div v-if="!mealId">
          <label class="text-label-lg font-bold text-on-background mb-2 block">날짜</label>
          <input
            v-model="recordForm.date"
            type="date"
            required
            class="w-full px-4 py-3 neo-brutal-border rounded-xl text-body-md bg-white focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>

        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">섭취량</label>
          <div class="flex items-center gap-3">
            <input
              v-model="recordForm.amountGrams"
              type="number"
              min="1"
              step="1"
              required
              class="flex-1 px-4 py-3 neo-brutal-border rounded-xl text-body-md text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
            />
            <span class="text-body-md font-bold text-on-surface-variant shrink-0">g</span>
          </div>
          <p class="text-label-lg text-primary mt-2 font-bold">
            ≈ {{ formatNumber(selectedFood.caloriesPer100g * recordForm.amountGrams / 100) }} kcal
          </p>
        </div>

        <p v-if="recordState.error" class="text-danger text-label-lg font-bold p-3 bg-surface neo-brutal-border rounded-xl">
          {{ recordState.error }}
        </p>

        <div
          v-if="recordState.success"
          class="flex items-center gap-2 text-success text-label-lg font-bold p-3 bg-surface neo-brutal-border rounded-xl"
        >
          <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL' 1;">check_circle</span>
          식단이 기록되었습니다.
        </div>

        <button
          type="submit"
          :disabled="recordState.loading"
          class="w-full py-4 bg-primary text-on-primary neo-brutal-border neo-brutal-shadow rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-transform disabled:opacity-50 flex items-center justify-center gap-2"
        >
          <span v-if="recordState.loading" class="material-symbols-outlined animate-spin">progress_activity</span>
          <span v-else class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">save</span>
          {{ recordState.loading ? '저장 중...' : '기록 저장' }}
        </button>
      </form>
    </div>

    <div
      v-else-if="(isManualMode || mealId) && !selectedFood && foods.length > 0"
      class="bg-surface neo-brutal-border rounded-xl p-8 text-center text-on-surface-variant"
    >
      <span class="material-symbols-outlined text-4xl opacity-30 block mb-2" style="font-variation-settings:'FILL' 1;">touch_app</span>
      <p class="text-body-md">위 목록에서 음식을 선택하면 기록 폼이 나타납니다.</p>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { addMealItem, recordMeal, searchFoods } from '@/api/dashboard';
import { useBadgeStore } from '@/stores/badge';
import { getEffectiveToday } from '@/utils/effectiveDate';

const route = useRoute();
const router = useRouter();
const badgeStore = useBadgeStore();
const todayDate = formatDate(getEffectiveToday());
const isManualMode = computed(() => route.meta.mode === 'manual');
const mealId = computed(() => route.query.mealId ? Number(route.query.mealId) : null);
const query = ref(typeof route.query.q === 'string' ? route.query.q : '');
const foods = ref([]);
const selectedFood = ref(null);

const searchState = reactive({
  loading: false,
  error: '',
  success: false,
});
const recordState = reactive({
  loading: false,
  error: '',
  success: false,
});
const recordForm = reactive({
  type: 'DINNER',
  date: todayDate,
  foodCode: '',
  amountGrams: 100,
});

const requestPreview = computed(() => JSON.stringify({
  type: recordForm.type,
  date: recordForm.date,
  items: [
    {
      foodCode: recordForm.foodCode || '',
      amountGrams: Number(recordForm.amountGrams || 0),
    },
  ],
}, null, 2));

onMounted(() => {
  if (query.value) {
    handleSearch();
  }
});

watch(selectedFood, (food) => {
  if (food) {
    recordForm.foodCode = food.foodCode;
  }
});

async function handleSearch() {
  searchState.loading = true;
  searchState.error = '';
  searchState.success = false;

  try {
    foods.value = await searchFoods(query.value);
    searchState.success = true;
  } catch (error) {
    searchState.error = formatApiError(error, 'Food 검색 API 호출에 실패했습니다.');
    foods.value = [];
  } finally {
    searchState.loading = false;
  }
}

async function handleRecord() {
  recordState.loading = true;
  recordState.error = '';
  recordState.success = false;

  try {
    if (mealId.value) {
      await addMealItem(mealId.value, { foodCode: recordForm.foodCode, amountGrams: recordForm.amountGrams });
      router.push('/log');
    } else {
      const meal = await recordMeal(recordForm);
      badgeStore.celebrate(meal);
      recordState.success = true;
    }
  } catch (error) {
    recordState.error = formatApiError(error, 'Meal 저장 API 호출에 실패했습니다.');
  } finally {
    recordState.loading = false;
  }
}

function selectFood(food) {
  selectedFood.value = food;
}

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

function formatNumber(value) {
  return Math.round(Number(value) || 0).toLocaleString('ko-KR');
}

function formatApiError(error, fallbackMessage) {
  if (error?.status === 401) {
    return '로그인이 필요합니다. 다시 로그인해 주세요.';
  }

  return error?.data?.message ?? fallbackMessage;
}
</script>
