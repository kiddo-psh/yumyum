<template>
  <section class="meal-action">
    <header class="meal-action__header">
      <RouterLink
        :to="mealId ? '/log' : '/'"
        class="back-link"
      >
        {{ mealId ? '식단 기록으로' : '대시보드로' }}
      </RouterLink>
      <h1>{{ mealId ? '음식 추가' : isManualMode ? '직접 입력' : '음식 검색' }}</h1>
      <p>
        {{ mealId ? '기존 끼니에 음식을 추가합니다.' : isManualMode ? 'Food를 검색해 MealRequest 형식으로 식단을 저장합니다.' : 'GET /foods?query=... 형식으로 음식 마스터 데이터를 조회합니다.' }}
      </p>
    </header>

    <form
      class="search-box"
      @submit.prevent="handleSearch"
    >
      <label for="food-query">검색어</label>
      <div>
        <input
          id="food-query"
          v-model.trim="query"
          type="search"
          placeholder="예: 닭가슴살"
        >
        <button
          type="submit"
          :disabled="searchState.loading"
        >
          {{ searchState.loading ? '검색 중' : '검색' }}
        </button>
      </div>
    </form>

    <p
      v-if="searchState.error"
      class="status status--error"
    >
      {{ searchState.error }}
    </p>
    <p
      v-else-if="searchState.success"
      class="status status--success"
    >
      {{ foods.length }}개 Food를 불러왔습니다.
    </p>

    <div class="meal-action__grid">
      <article class="food-list">
        <h2>검색 결과</h2>
        <p
          v-if="searchState.loading"
          class="empty-state"
        >
          서버에서 Food 목록을 가져오는 중입니다.
        </p>
        <p
          v-else-if="!foods.length"
          class="empty-state"
        >
          검색 결과가 없습니다.
        </p>
        <button
          v-for="food in foods"
          v-else
          :key="food.id"
          type="button"
          class="food-row"
          :class="{ 'food-row--selected': selectedFood?.id === food.id }"
          @click="selectFood(food)"
        >
          <span>
            <strong>{{ food.name }}</strong>
            <small>100g 기준 {{ formatNumber(food.caloriesPer100g) }} kcal</small>
          </span>
          <b>P {{ formatNumber(food.proteinPer100g) }}g</b>
        </button>
      </article>

      <article
        v-if="isManualMode"
        class="record-panel"
      >
        <h2>Meal 저장</h2>
        <form @submit.prevent="handleRecord">
          <label>
            Meal 타입
            <select v-model="recordForm.type">
              <option value="BREAKFAST">BREAKFAST</option>
              <option value="LUNCH">LUNCH</option>
              <option value="DINNER">DINNER</option>
              <option value="SNACK">SNACK</option>
            </select>
          </label>
          <label>
            날짜
            <input
              v-model="recordForm.date"
              type="date"
              required
            >
          </label>
          <label>
            Food Code
            <input
              v-model="recordForm.foodCode"
              type="text"
              required
            >
          </label>
          <label>
            섭취량(g)
            <input
              v-model="recordForm.amountGrams"
              type="number"
              min="1"
              step="1"
              required
            >
          </label>
          <button
            class="submit-button"
            type="submit"
            :disabled="recordState.loading"
          >
            {{ recordState.loading ? '저장 중' : 'Meal 저장' }}
          </button>
        </form>

        <p
          v-if="recordState.error"
          class="status status--error"
        >
          {{ recordState.error }}
        </p>
        <p
          v-else-if="recordState.success"
          class="status status--success"
        >
          Meal이 저장되었습니다. 대시보드에서 오늘 기록을 다시 확인하세요.
        </p>

        <pre class="request-preview">{{ requestPreview }}</pre>
      </article>
    </div>
  </section>
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
    return '로그인이 필요합니다. Authorization: Bearer accessToken 헤더가 필요합니다.';
  }

  return error?.data?.message ?? fallbackMessage;
}
</script>

<style scoped>
.meal-action {
  display: grid;
  gap: 24px;
}

.meal-action__header {
  display: grid;
  gap: 6px;
}

.back-link {
  width: fit-content;
  color: var(--color-success);
  font-weight: 800;
  text-decoration: none;
}

.meal-action__header h1,
.food-list h2,
.record-panel h2 {
  margin: 0;
  color: var(--color-text-strong);
}

.meal-action__header p {
  margin: 0;
  color: var(--color-text-muted);
}

.search-box,
.food-list,
.record-panel {
  padding: 24px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-2xl);
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.search-box {
  display: grid;
  gap: 10px;
}

.search-box label,
.record-panel label {
  color: var(--color-text-strong);
  font-weight: 800;
}

.search-box div {
  display: flex;
  gap: 12px;
}

input,
select {
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  color: var(--color-text-strong);
}

button {
  min-height: 44px;
  border: 2px solid #1e5000;
  border-radius: var(--radius-lg);
  background: var(--color-primary);
  box-shadow: var(--shadow-press-green);
  color: #1e5000;
  font-weight: 800;
}

button:active {
  transform: translateY(3px);
  box-shadow: none;
}

.search-box button {
  min-width: 96px;
}

.meal-action__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.food-list {
  display: grid;
  gap: 12px;
}

.food-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-color: #e5e5e5;
  background: var(--color-surface);
  box-shadow: 0 3px 0 #e5e5e5;
  color: var(--color-text);
  text-align: left;
}

.food-row--selected {
  border-color: var(--color-accent-strong);
  background: var(--color-accent);
  box-shadow: var(--shadow-press-yellow);
  color: var(--color-accent-strong);
}

.food-row strong,
.food-row small {
  display: block;
}

.food-row small {
  color: var(--color-text-muted);
}

.food-row b {
  white-space: nowrap;
}

.record-panel form {
  display: grid;
  gap: 14px;
}

.record-panel label {
  display: grid;
  gap: 6px;
}

.submit-button {
  margin-top: 4px;
}

.request-preview {
  overflow: auto;
  margin: 16px 0 0;
  padding: 14px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  color: var(--color-text);
}

.status,
.empty-state {
  margin: 0;
  padding: 12px 14px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: #fff;
  font-weight: 700;
}

.status--error {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.status--success {
  border-color: var(--color-primary);
  color: var(--color-success);
}

@media (max-width: 860px) {
  .search-box div,
  .meal-action__grid {
    grid-template-columns: 1fr;
  }

  .search-box div {
    display: grid;
  }
}
</style>
