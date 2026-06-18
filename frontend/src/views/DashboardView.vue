<template>
  <section
    class="dashboard"
    aria-label="YumYum dashboard"
  >
    <div class="dashboard__main">
      <header class="dashboard__greeting">
        <div>
          <h1>안녕하세요, 지민님!</h1>
          <p class="streak-pill">
            <span aria-hidden="true" />
            오늘 {{ currentStreak }}일째 성공 중이에요!
          </p>
        </div>
        <button
          class="icon-button"
          type="button"
          aria-label="알림"
        >
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
            <path d="M10 21h4" />
          </svg>
        </button>
      </header>

      <p
        v-if="dashboardState.loading"
        class="status-banner status-banner--loading"
      >
        오늘의 대시보드를 불러오는 중입니다.
      </p>
      <p
        v-else-if="dashboardState.error"
        class="status-banner status-banner--error"
      >
        {{ dashboardState.error }}
      </p>

      <article class="quest-card">
        <div class="quest-card__header">
          <h2>
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path d="M12 3v7" />
              <path d="M8 7a4 4 0 1 0 8 0" />
              <path d="M7 20h10" />
            </svg>
            오늘의 식단 퀘스트
          </h2>
          <span>목표: {{ formatKcal(targetCalories) }}</span>
        </div>

        <div
          class="calorie-ring"
          :style="{ '--progress': calorieProgress }"
        >
          <div class="calorie-ring__track">
            <div class="calorie-ring__center">
              <strong>{{ formatNumber(intakeCalories) }}</strong>
              <span>kcal 섭취</span>
              <small>{{ formatNumber(remainingCalories) }} kcal 남음</small>
            </div>
          </div>
        </div>
      </article>

      <div
        class="nutrient-grid"
        aria-label="오늘의 영양소"
      >
        <article
          v-for="nutrient in nutrients"
          :key="nutrient.key"
          class="nutrient-card"
        >
          <div class="nutrient-card__top">
            <span :style="{ color: nutrient.color }">{{ nutrient.label }}</span>
            <strong>
              {{ formatGram(nutrient.current) }}
              <small>/ {{ formatGram(nutrient.target) }}</small>
            </strong>
          </div>
          <div class="progress-bar">
            <span :style="{ width: `${nutrient.percent}%`, background: nutrient.color }" />
          </div>
        </article>
      </div>

      <div class="quick-actions">
        <RouterLink
          class="press-button press-button--light"
          to="/meals/search"
        >
          <span
            class="round-icon"
            aria-hidden="true"
          >
            <svg viewBox="0 0 24 24"><circle
              cx="11"
              cy="11"
              r="7"
            /><path d="m16 16 4 4" /></svg>
          </span>
          음식 검색
        </RouterLink>
        <RouterLink
          class="press-button press-button--primary"
          to="/meals/photo"
        >
          <span
            class="round-icon"
            aria-hidden="true"
          >
            <svg viewBox="0 0 24 24"><path d="M4 8h4l2-3h4l2 3h4v11H4z" /><circle
              cx="12"
              cy="14"
              r="3"
            /></svg>
          </span>
          사진으로 기록
        </RouterLink>
        <RouterLink
          class="press-button press-button--light"
          to="/meals/manual"
        >
          <span
            class="round-icon"
            aria-hidden="true"
          >
            <svg viewBox="0 0 24 24"><path d="m4 20 4-1 11-11-3-3L5 16z" /><path d="m14 6 3 3" /></svg>
          </span>
          직접 입력
        </RouterLink>
      </div>

      <article class="meal-log">
        <div class="section-title">
          <h2>식단 기록</h2>
          <RouterLink to="/meals/manual">
            추가하기
          </RouterLink>
        </div>

        <div
          v-if="dashboardState.loading"
          class="empty-state"
        >
          Meal 목록을 확인하고 있습니다.
        </div>
        <ol
          v-else-if="mealTimeline.length"
          class="timeline"
        >
          <li
            v-for="meal in mealTimeline"
            :key="meal.id"
          >
            <span
              class="timeline__dot"
              aria-hidden="true"
            />
            <div class="timeline__card">
              <div>
                <strong>{{ meal.label }}</strong>
                <span>{{ meal.description }}</span>
              </div>
              <b>{{ formatKcal(meal.calories) }}</b>
            </div>
          </li>
        </ol>
        <div
          v-else
          class="empty-state"
        >
          아직 등록된 Meal이 없습니다. 직접 입력으로 첫 끼니를 기록하세요.
        </div>
      </article>
    </div>

    <aside
      class="dashboard__side"
      aria-label="AI coaching panel"
    >
      <article class="coach-card">
        <div class="speech-bubble">
          {{ coachMessage }}
        </div>
      </article>

      <article class="recommend-card">
        <div class="section-title">
          <h2>마지막 끼니 추천</h2>
          <button
            type="button"
            class="mini-button"
            :disabled="recommendationState.loading"
            @click="loadRecommendation"
          >
            {{ recommendationState.loading ? '요청 중' : '추천 받기' }}
          </button>
        </div>

        <p
          v-if="!canRecommendLastMeal"
          class="hint"
        >
          조건이 충족되면 추천을 바로 받을 수 있습니다.
        </p>
        <p
          v-if="recommendationState.error"
          class="status-banner status-banner--error"
        >
          {{ recommendationState.error }}
        </p>
        <p
          v-else-if="recommendationState.success"
          class="status-banner status-banner--success"
        >
          추천을 불러왔습니다. 다음 행동을 선택하세요.
        </p>

        <div class="recommend-card__body">
          <h3>{{ primaryRecommendation.name }}</h3>
          <div class="tag-row">
            <span>{{ priorityLabel }}</span>
            <span>{{ primaryRecommendation.reason || '균형 보완' }}</span>
          </div>
          <div class="recommend-card__footer">
            <div>
              <span>예상 칼로리</span>
              <strong>{{ formatKcal(primaryRecommendation.kcal) }}</strong>
            </div>
            <RouterLink
              class="small-press-button"
              :to="{ name: 'meal-manual', query: { q: primaryRecommendation.name } }"
            >
              식단 등록
            </RouterLink>
          </div>
        </div>
      </article>

      <article class="weekly-card">
        <h2>주간 달성률</h2>
        <div class="week-bars">
          <div
            v-for="day in weekDays"
            :key="day.date"
            class="week-bars__item"
          >
            <span
              class="week-bars__bar"
              :class="{ 'week-bars__bar--today': day.isToday }"
            >
              <b :style="{ height: `${day.percent}%`, background: day.color }" />
            </span>
            <small>{{ day.label }}</small>
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue';

import {
  getCalorieBalance,
  getDailyProgress,
  getDailySummary,
  getLastMealRecommendation,
  getWeeklyCalendar,
  listMeals,
} from '@/api/dashboard';

const macroTargets = {
  carbs: 250,
  protein: 120,
  fat: 60,
};
const mealTypeLabels = {
  BREAKFAST: '아침',
  LUNCH: '점심',
  DINNER: '저녁',
  SNACK: '간식',
};
const dayLabels = ['일', '월', '화', '수', '목', '금', '토'];

const dashboardState = reactive({
  loading: true,
  error: '',
  summary: null,
  balance: null,
  meals: [],
  weeklyCalendar: null,
  progress: null,
});

const recommendationState = reactive({
  loading: false,
  error: '',
  success: false,
  response: null,
});

const today = new Date();
const todayDate = formatDate(today);

const targetCalories = computed(() => dashboardState.balance?.targetCalories ?? dashboardState.summary?.targetCalories ?? 0);
const intakeCalories = computed(() => dashboardState.balance?.intakeCalories ?? dashboardState.summary?.achievedCalories ?? 0);
const remainingCalories = computed(() => Math.max(dashboardState.balance?.remainingCalories ?? targetCalories.value - intakeCalories.value, 0));
const currentStreak = computed(() => dashboardState.summary?.currentStreak ?? 0);
const canRecommendLastMeal = computed(() => Boolean(dashboardState.balance?.lastMealRecommendTrigger));
const calorieProgress = computed(() => clampPercent(targetCalories.value ? (intakeCalories.value / targetCalories.value) * 100 : 0));

const nutrients = computed(() => [
  {
    key: 'carbs',
    label: '탄수화물',
    current: dashboardState.summary?.totalCarbs ?? 0,
    target: macroTargets.carbs,
    color: '#fed33a',
  },
  {
    key: 'protein',
    label: '단백질',
    current: dashboardState.summary?.totalProtein ?? 0,
    target: macroTargets.protein,
    color: '#4abdff',
  },
  {
    key: 'fat',
    label: '지방',
    current: dashboardState.summary?.totalFat ?? 0,
    target: macroTargets.fat,
    color: '#58cc02',
  },
].map((nutrient) => ({
  ...nutrient,
  percent: clampPercent((nutrient.current / nutrient.target) * 100),
})));

const mealTimeline = computed(() => dashboardState.meals.map((meal) => {
  const calories = meal.items.reduce((total, item) => total + item.calories, 0);
  const amount = meal.items.reduce((total, item) => total + item.amountGrams, 0);

  return {
    id: meal.id,
    label: mealTypeLabels[meal.type] ?? meal.type,
    description: `${meal.items.length}개 음식 · ${formatNumber(amount)}g`,
    calories,
  };
}));

const primaryRecommendation = computed(() => {
  const recommendation = recommendationState.response?.recommendations?.[0];

  return recommendation ?? {
    name: '추천을 요청해 주세요',
    kcal: Math.max(Math.round(remainingCalories.value), 0),
    reason: canRecommendLastMeal.value ? '잔여 영양소 기반' : '조건 대기',
  };
});

const priorityLabel = computed(() => {
  const nutrient = recommendationState.response?.priorityNutrient;

  if (!nutrient) {
    return 'AI 추천';
  }

  return `${nutrient} 보완`;
});

const coachMessage = computed(() => {
  if (dashboardState.error) {
    return '데이터 연결을 확인해 주세요. 토큰이 있으면 다시 시도할 수 있어요.';
  }

  if (canRecommendLastMeal.value) {
    return '마지막 끼니에서 부족한 영양소를 채워볼까요?';
  }

  return `${formatKcal(remainingCalories.value)} 남았어요. 다음 Meal을 기록하면 코칭이 더 정확해져요.`;
});

const weekDays = computed(() => {
  if (!dashboardState.weeklyCalendar?.days?.length) {
    return dayLabels.map((label, index) => ({
      date: `empty-${label}`,
      label,
      percent: index < 4 ? [100, 80, 60, 68][index] : 0,
      color: index === 2 ? '#fed33a' : '#58cc02',
      isToday: index === 3,
    }));
  }

  return dashboardState.weeklyCalendar.days.map((day) => {
    const date = new Date(`${day.date}T00:00:00`);
    const percent = clampPercent((day.achievementRate ?? 0) * 100);

    return {
      date: day.date,
      label: day.date === todayDate ? '오늘' : dayLabels[date.getDay()],
      percent,
      color: day.status === 'PARTIAL' ? '#fed33a' : '#58cc02',
      isToday: day.date === todayDate,
    };
  });
});

onMounted(loadDashboard);

async function loadDashboard() {
  dashboardState.loading = true;
  dashboardState.error = '';

  const [summary, balance, meals, weeklyCalendar, progress] = await Promise.allSettled([
    getDailySummary(todayDate),
    getCalorieBalance(todayDate),
    listMeals(todayDate),
    getWeeklyCalendar(),
    getDailyProgress(),
  ]);

  applySettledValue(summary, 'summary');
  applySettledValue(balance, 'balance');
  applySettledValue(meals, 'meals', []);
  applySettledValue(weeklyCalendar, 'weeklyCalendar');
  applySettledValue(progress, 'progress');

  const failed = [summary, balance, meals, weeklyCalendar, progress].find((result) => result.status === 'rejected');
  if (failed) {
    dashboardState.error = formatApiError(failed.reason, '대시보드 API 호출에 실패했습니다.');
  }

  dashboardState.loading = false;
}

async function loadRecommendation() {
  recommendationState.loading = true;
  recommendationState.error = '';
  recommendationState.success = false;

  try {
    recommendationState.response = await getLastMealRecommendation();
    recommendationState.success = true;
  } catch (error) {
    recommendationState.error = formatApiError(error, '마지막 끼니 추천 API 호출에 실패했습니다.');
  } finally {
    recommendationState.loading = false;
  }
}

function applySettledValue(result, key, fallback = null) {
  dashboardState[key] = result.status === 'fulfilled' ? result.value : fallback;
}

function clampPercent(value) {
  if (!Number.isFinite(value)) {
    return 0;
  }

  return Math.max(0, Math.min(Math.round(value), 100));
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

function formatKcal(value) {
  return `${formatNumber(value)} kcal`;
}

function formatGram(value) {
  return `${formatNumber(value)}g`;
}

function formatApiError(error, fallbackMessage) {
  if (error?.status === 401) {
    return '로그인이 필요합니다. Authorization: Bearer accessToken 헤더가 필요합니다.';
  }

  return error?.data?.message ?? fallbackMessage;
}
</script>

<style scoped>
.dashboard {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 0.95fr);
  gap: 32px;
}

.dashboard__main,
.dashboard__side {
  display: grid;
  align-content: start;
  gap: 24px;
}

.dashboard__greeting,
.quest-card__header,
.section-title,
.nutrient-card__top,
.recommend-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dashboard__greeting h1 {
  margin: 0 0 4px;
  color: var(--color-text-strong);
  font-size: 1rem;
  line-height: 1.5;
}

.streak-pill,
.quest-card__header span,
.calorie-ring__center small,
.tag-row span,
.hint {
  border: 2px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-panel);
}

.streak-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 6px 10px;
  border-color: var(--color-accent-strong);
  background: var(--color-accent);
  color: var(--color-accent-strong);
  font-weight: 700;
}

.streak-pill span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.icon-button,
.mini-button,
.small-press-button,
.press-button {
  border: 2px solid #1e5000;
  background: var(--color-primary);
  box-shadow: var(--shadow-press-green);
  color: #1e5000;
  font-weight: 800;
  text-decoration: none;
  transition:
    transform 150ms ease,
    box-shadow 150ms ease,
    filter 150ms ease;
}

.icon-button:active,
.mini-button:active,
.small-press-button:active,
.press-button:active {
  transform: translateY(3px);
  box-shadow: none;
}

.icon-button {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-color: var(--color-border);
  border-radius: 999px;
  background: var(--color-panel);
  box-shadow: none;
  color: var(--color-text-strong);
}

.icon-button svg,
.quest-card__header svg,
.round-icon svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.5;
}

.quest-card,
.meal-log,
.coach-card,
.recommend-card,
.weekly-card {
  border: 2px solid var(--color-border);
  border-radius: var(--radius-2xl);
  background: var(--color-surface-raised);
  box-shadow: var(--shadow-sm);
}

.quest-card {
  position: relative;
  overflow: hidden;
  padding: 26px;
  border-color: var(--color-success);
  background:
    radial-gradient(circle at 90% 10%, rgb(88 204 2 / 18%), transparent 22%),
    radial-gradient(circle at 5% 90%, rgb(254 211 58 / 20%), transparent 20%),
    linear-gradient(148deg, #fbf9f8, #f5f3f3);
  box-shadow: 0 4px 0 var(--color-success);
}

.quest-card__header h2,
.section-title h2,
.weekly-card h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--color-text-strong);
  font-size: 1rem;
}

.quest-card__header span {
  padding: 6px 18px;
}

.calorie-ring {
  --progress: 0;
  display: grid;
  min-height: 288px;
  place-items: center;
}

.calorie-ring__track {
  display: grid;
  width: 256px;
  height: 256px;
  place-items: center;
  border-radius: 50%;
  background:
    radial-gradient(circle, #fbf9f8 0 52%, transparent 53%),
    conic-gradient(var(--color-primary) calc(var(--progress) * 1%), #e4e2e2 0);
  box-shadow: inset 0 0 0 6px #fed33a;
}

.calorie-ring__center {
  display: grid;
  justify-items: center;
  color: var(--color-text);
}

.calorie-ring__center strong {
  color: var(--color-success);
  font-size: 1.5rem;
}

.calorie-ring__center small {
  margin-top: 8px;
  padding: 6px 10px;
  color: var(--color-text-strong);
}

.nutrient-grid,
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.nutrient-card {
  min-height: 104px;
  padding: 18px;
  border: 2px solid #e5e5e5;
  border-radius: var(--radius-2xl);
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.nutrient-card strong {
  color: var(--color-text-strong);
}

.nutrient-card small {
  color: var(--color-text);
}

.progress-bar {
  height: 16px;
  margin-top: 14px;
  padding: 2px;
  border: 2px solid var(--color-border);
  border-radius: 999px;
  background: #e4e2e2;
}

.progress-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.press-button {
  display: grid;
  min-height: 92px;
  place-items: center;
  gap: 8px;
  padding: 18px;
  border-color: #e5e5e5;
  border-radius: var(--radius-2xl);
  background: #fff;
  box-shadow: var(--shadow-sm);
  color: var(--color-text-strong);
}

.press-button--primary {
  border-color: #1e5000;
  border-radius: var(--radius-xl);
  background: var(--color-primary);
  box-shadow: var(--shadow-press-green);
  color: #1e5000;
}

.round-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 2px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-panel);
}

.meal-log,
.recommend-card,
.weekly-card {
  padding: 18px;
}

.section-title a {
  color: var(--color-success);
  font-weight: 800;
  text-decoration: none;
}

.timeline {
  display: grid;
  gap: 14px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.timeline li {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  align-items: start;
}

.timeline__dot {
  width: 28px;
  height: 28px;
  margin: 10px auto 0;
  border: 2px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-panel);
}

.timeline__card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 68px;
  padding: 16px 18px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.timeline__card strong,
.timeline__card span {
  display: block;
}

.timeline__card span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.timeline__card b {
  color: var(--color-success);
  white-space: nowrap;
}

.coach-card {
  display: grid;
  justify-items: center;
  gap: 16px;
  padding: 26px 18px;
  border-color: #f3bf00;
  background: #fff5c7;
  box-shadow: 0 4px 0 #f3bf00;
}

.speech-bubble {
  position: relative;
  max-width: 220px;
  padding: 18px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  color: var(--color-text-strong);
  text-align: center;
}

.speech-bubble::after {
  position: absolute;
  bottom: -10px;
  left: calc(50% - 8px);
  width: 16px;
  height: 16px;
  border-right: 2px solid var(--color-border);
  border-bottom: 2px solid var(--color-border);
  background: var(--color-surface);
  content: "";
  transform: rotate(45deg);
}

.mascot {
  width: 128px;
  height: 128px;
  object-fit: contain;
  filter: drop-shadow(0 14px 12px rgb(0 0 0 / 10%));
}

.recommend-card {
  border-color: var(--color-info);
  box-shadow: 0 4px 0 var(--color-info);
  background: linear-gradient(#fbf9f8, #f5f3f3);
}

.mini-button {
  padding: 6px 12px;
  border-radius: var(--radius-lg);
}

.mini-button:disabled {
  cursor: wait;
  filter: grayscale(0.4);
}

.hint {
  padding: 8px 12px;
  font-size: var(--font-size-sm);
}

.recommend-card__body {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  padding: 18px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.recommend-card__body h3 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 1.125rem;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-row span {
  padding: 5px;
  border-width: 1px;
  font-size: 0.75rem;
}

.recommend-card__footer {
  padding-top: 14px;
  border-top: 2px dashed var(--color-border);
}

.recommend-card__footer span,
.recommend-card__footer strong {
  display: block;
}

.recommend-card__footer span {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.recommend-card__footer strong {
  color: #006590;
}

.small-press-button {
  padding: 10px 18px;
  border-radius: var(--radius-lg);
}

.week-bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  align-items: end;
  margin-top: 16px;
}

.week-bars__item {
  display: grid;
  justify-items: center;
  gap: 4px;
}

.week-bars__bar {
  display: flex;
  align-items: end;
  width: 32px;
  height: 96px;
  padding: 2px;
  border: 2px dashed var(--color-border);
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  background: #efeded;
}

.week-bars__bar--today {
  border-style: solid;
  border-color: var(--color-success);
}

.week-bars__bar b {
  width: 100%;
  min-height: 2px;
  border-radius: 3px 3px 0 0;
}

.status-banner,
.empty-state {
  margin: 0;
  padding: 12px 14px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: #fff;
  color: var(--color-text);
  font-weight: 700;
}

.status-banner--loading {
  border-color: var(--color-info);
}

.status-banner--error {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.status-banner--success {
  border-color: var(--color-primary);
  color: var(--color-success);
}

@media (max-width: 1100px) {
  .dashboard {
    grid-template-columns: 1fr;
  }

  .dashboard__side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .weekly-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .dashboard__side,
  .nutrient-grid,
  .quick-actions {
    grid-template-columns: 1fr;
  }

  .dashboard__greeting,
  .quest-card__header,
  .recommend-card__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .calorie-ring {
    min-height: 220px;
  }

  .calorie-ring__track {
    width: 210px;
    height: 210px;
  }
}

.dashboard {
  grid-template-columns: minmax(0, 1fr) 342px;
  gap: 34px;
  max-width: 1172px;
}

.dashboard__main,
.dashboard__side {
  gap: 26px;
}

.dashboard__greeting h1 {
  margin: 0 0 10px;
  color: var(--color-text-strong);
  font-size: 1.45rem;
  font-weight: 950;
  line-height: 1.15;
}

.streak-pill {
  gap: 9px;
  padding: 9px 16px 10px;
  border-color: var(--color-accent-strong);
  background: var(--color-accent);
  box-shadow: 0 5px 0 #b98e00;
  color: var(--color-accent-strong);
  font-weight: 950;
}

.streak-pill span {
  width: 11px;
  height: 11px;
  box-shadow: 0 0 0 4px rgb(113 91 0 / 14%);
}

.icon-button,
.mini-button,
.small-press-button,
.press-button {
  border-width: 3px;
  border-color: var(--color-primary-contrast);
  color: var(--color-primary-contrast);
  font-weight: 950;
}

.icon-button:active,
.mini-button:active,
.small-press-button:active,
.press-button:active {
  transform: translateY(5px);
}

.icon-button {
  width: 52px;
  height: 52px;
  border-color: #b8cab0;
  background: #ffffff;
  box-shadow: 0 5px 0 #dbe7d2;
  color: var(--color-text-strong);
}

.icon-button svg,
.quest-card__header svg,
.round-icon svg {
  width: 22px;
  height: 22px;
  stroke-width: 3;
}

.quest-card,
.meal-log,
.coach-card,
.recommend-card,
.weekly-card {
  border-width: 3px;
  border-radius: var(--radius-2xl);
}

.quest-card {
  min-height: 420px;
  padding: 30px;
  border-color: var(--color-border-strong);
  background:
    radial-gradient(circle at 89% 17%, rgb(88 204 2 / 18%), transparent 22%),
    radial-gradient(circle at 13% 88%, rgb(254 211 58 / 24%), transparent 23%),
    linear-gradient(145deg, #ffffff 0%, #fbfff5 58%, #fff9d7 100%);
  box-shadow: 0 8px 0 var(--color-border-strong);
}

.quest-card__header h2,
.section-title h2,
.weekly-card h2 {
  gap: 10px;
  font-size: 1.125rem;
  font-weight: 950;
}

.quest-card__header span {
  padding: 9px 18px;
  border-width: 3px;
  background: #ffffff;
  box-shadow: 0 4px 0 #dfe7d9;
  color: var(--color-text);
  font-weight: 900;
}

.calorie-ring {
  min-height: 326px;
}

.calorie-ring__track {
  width: 292px;
  height: 292px;
  border: 8px solid #ffffff;
  background:
    radial-gradient(circle, #ffffff 0 48%, transparent 49%),
    conic-gradient(var(--color-primary) calc(var(--progress) * 1%), #e7ece3 0);
  box-shadow:
    inset 0 0 0 11px var(--color-accent),
    0 12px 0 rgb(47 95 24 / 14%);
}

.calorie-ring__center {
  font-weight: 850;
}

.calorie-ring__center strong {
  color: var(--color-success);
  font-size: 2.4rem;
  font-weight: 950;
  line-height: 1;
}

.calorie-ring__center small {
  margin-top: 12px;
  padding: 7px 13px 8px;
  border-width: 3px;
  background: #ffffff;
  box-shadow: 0 4px 0 #dfe7d9;
  font-weight: 950;
}

.nutrient-grid,
.quick-actions {
  gap: 18px;
}

.nutrient-card {
  min-height: 112px;
  padding: 20px;
  border: 3px solid #e0e8da;
  border-radius: 22px;
  box-shadow: 0 7px 0 #e3e9df;
}

.nutrient-card strong {
  font-size: 1.1rem;
  font-weight: 950;
}

.nutrient-card small {
  font-weight: 850;
}

.progress-bar {
  height: 20px;
  margin-top: 18px;
  padding: 3px;
  border-width: 3px;
  background: var(--color-track);
}

.progress-bar span {
  min-width: 5px;
}

.press-button {
  min-height: 112px;
  gap: 10px;
  padding: 20px;
  border-color: #dfe8d8;
  border-radius: 24px;
  box-shadow: 0 7px 0 #dfe7d9;
  font-size: 1.05rem;
}

.press-button--primary {
  border-color: var(--color-primary-contrast);
  border-radius: 24px;
  background: var(--color-primary);
  box-shadow: var(--shadow-press-green);
}

.round-icon {
  width: 54px;
  height: 54px;
  border-width: 3px;
  background: #ffffff;
  box-shadow: inset 0 -4px 0 #e8efe2;
}

.meal-log,
.recommend-card,
.weekly-card {
  padding: 22px;
}

.section-title a {
  font-weight: 950;
}

.timeline__dot {
  width: 30px;
  height: 30px;
  border-width: 3px;
  background: var(--color-accent);
  box-shadow: 0 4px 0 #b98e00;
}

.timeline__card {
  border-width: 3px;
  border-radius: 18px;
  background: #ffffff;
}

.coach-card {
  gap: 18px;
  min-height: 336px;
  padding: 26px 20px 24px;
  border-color: #dba900;
  background:
    radial-gradient(circle at 82% 22%, rgb(255 255 255 / 72%), transparent 42px),
    linear-gradient(180deg, #fff7c9 0%, #fff1a8 100%);
  box-shadow: 0 8px 0 #dba900;
}

.speech-bubble {
  max-width: 260px;
  padding: 18px 20px;
  border-width: 3px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 5px 0 #dfe7d9;
  font-weight: 850;
  line-height: 1.6;
}

.speech-bubble::after {
  border-right-width: 3px;
  border-bottom-width: 3px;
  background: #ffffff;
}

.mascot {
  width: 154px;
  height: 154px;
}

.recommend-card {
  border-color: var(--color-info);
  background:
    radial-gradient(circle at 90% 18%, rgb(74 189 255 / 20%), transparent 54px),
    linear-gradient(180deg, #ffffff 0%, #f5fbff 100%);
  box-shadow: var(--shadow-press-blue);
}

.mini-button {
  padding: 10px 16px 11px;
  border-radius: 16px;
}

.hint {
  padding: 12px 15px;
  border-width: 3px;
  background: #ffffff;
  font-weight: 850;
}

.recommend-card__body {
  gap: 14px;
  margin-top: 16px;
  padding: 20px;
  border-width: 3px;
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 6px 0 #dfe7d9;
}

.recommend-card__body h3 {
  font-size: 1.2rem;
  font-weight: 950;
}

.tag-row {
  gap: 6px;
}

.tag-row span {
  padding: 6px 9px;
  border-width: 2px;
  background: #f8fbf4;
  font-size: 0.75rem;
  font-weight: 850;
}

.recommend-card__footer {
  border-top-width: 3px;
}

.recommend-card__footer span {
  font-weight: 850;
}

.recommend-card__footer strong {
  color: #006590;
  font-size: 1.25rem;
  font-weight: 950;
}

.small-press-button {
  padding: 13px 19px 14px;
  border-radius: 17px;
}

.weekly-card {
  background:
    radial-gradient(circle at 13% 16%, rgb(88 204 2 / 12%), transparent 40px),
    #ffffff;
}

.week-bars {
  gap: 9px;
  margin-top: 18px;
}

.week-bars__item {
  gap: 7px;
  color: var(--color-text);
  font-weight: 950;
}

.week-bars__bar {
  width: 36px;
  height: 112px;
  padding: 3px;
  border-width: 3px;
  border-bottom: 3px dashed var(--color-border);
  border-radius: 14px;
  background: #f6f8f4;
  box-shadow: inset 0 -4px 0 #e7eee1;
}

.week-bars__bar--today {
  border-style: solid;
  border-color: var(--color-border-strong);
  background: #fbfff5;
}

.week-bars__bar b {
  border-radius: 9px;
  box-shadow: inset 0 -4px 0 rgb(0 0 0 / 9%);
}

.status-banner,
.empty-state {
  padding: 14px 16px;
  border-width: 3px;
  border-radius: 18px;
  box-shadow: 0 5px 0 #e3e9df;
  font-weight: 850;
}

.status-banner--loading {
  color: #006590;
}

@media (max-width: 1100px) {
  .dashboard {
    grid-template-columns: 1fr;
    max-width: none;
  }
}

@media (max-width: 720px) {
  .dashboard__side,
  .nutrient-grid,
  .quick-actions {
    grid-template-columns: 1fr;
  }

  .calorie-ring {
    min-height: 250px;
  }

  .calorie-ring__track {
    width: 226px;
    height: 226px;
  }

  .quest-card {
    min-height: 0;
    padding: 22px;
  }
}

/* Minimal dashboard direction: white canvas, restrained accents, no character art. */
.dashboard {
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: 28px;
  max-width: 1160px;
}

.dashboard__main,
.dashboard__side {
  gap: 20px;
}

.dashboard__greeting h1 {
  margin: 0 0 8px;
  font-size: 1.375rem;
  font-weight: 850;
  letter-spacing: 0;
}

.streak-pill,
.quest-card__header span,
.calorie-ring__center small,
.tag-row span,
.hint {
  border: 1px solid #d9e5d2;
  background: #ffffff;
  box-shadow: none;
}

.streak-pill {
  padding: 8px 14px;
  border-color: #d8b12a;
  background: #fff8d8;
  color: #6c5600;
  font-weight: 800;
}

.streak-pill span {
  width: 8px;
  height: 8px;
  box-shadow: none;
}

.icon-button,
.mini-button,
.small-press-button,
.press-button {
  border: 1px solid #cfe1c5;
  box-shadow: none;
  font-weight: 800;
  transition:
    background-color 140ms ease,
    border-color 140ms ease,
    transform 140ms ease;
}

.icon-button:active,
.mini-button:active,
.small-press-button:active,
.press-button:active {
  transform: translateY(1px);
}

.icon-button {
  width: 44px;
  height: 44px;
  background: #ffffff;
  color: var(--color-text-strong);
}

.icon-button:hover,
.press-button--light:hover {
  border-color: #9fc990;
  background: #f7fbf4;
}

.quest-card,
.meal-log,
.coach-card,
.recommend-card,
.weekly-card,
.nutrient-card {
  border: 1px solid #dfe8da;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 1px 0 rgb(23 32 24 / 6%);
}

.quest-card {
  min-height: 360px;
  padding: 28px;
  border-color: #b9d9ad;
  background: #ffffff;
}

.quest-card__header h2,
.section-title h2,
.weekly-card h2 {
  font-size: 1.05rem;
  font-weight: 850;
}

.quest-card__header svg,
.round-icon svg,
.icon-button svg {
  stroke-width: 2.4;
}

.quest-card__header span {
  padding: 7px 14px;
  color: var(--color-text);
  font-weight: 750;
}

.calorie-ring {
  min-height: 278px;
}

.calorie-ring__track {
  width: 244px;
  height: 244px;
  border: 0;
  background:
    radial-gradient(circle, #ffffff 0 55%, transparent 56%),
    conic-gradient(var(--color-primary) calc(var(--progress) * 1%), #eef2eb 0);
  box-shadow: none;
}

.calorie-ring__center {
  font-weight: 750;
}

.calorie-ring__center strong {
  font-size: 2rem;
  font-weight: 850;
}

.calorie-ring__center small {
  margin-top: 10px;
  padding: 6px 12px;
  font-weight: 750;
}

.nutrient-grid,
.quick-actions {
  gap: 14px;
}

.nutrient-card {
  min-height: 96px;
  padding: 18px;
}

.nutrient-card strong {
  font-size: 1rem;
  font-weight: 850;
}

.nutrient-card small {
  font-weight: 650;
}

.progress-bar {
  height: 12px;
  margin-top: 14px;
  padding: 0;
  border: 0;
  background: #edf2e9;
}

.progress-bar span {
  min-width: 0;
}

.press-button {
  min-height: 88px;
  padding: 18px;
  border-color: #dfe8da;
  border-radius: 18px;
  background: #ffffff;
  color: var(--color-text-strong);
  font-size: 1rem;
}

.press-button--primary,
.mini-button,
.small-press-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #163d00;
}

.press-button--primary:hover,
.mini-button:hover,
.small-press-button:hover {
  background: var(--color-primary-hover);
}

.round-icon {
  width: 42px;
  height: 42px;
  border: 1px solid #d9e5d2;
  background: #f8fbf5;
  box-shadow: none;
}

.meal-log,
.recommend-card,
.weekly-card,
.coach-card {
  padding: 20px;
}

.timeline__dot {
  width: 14px;
  height: 14px;
  margin-top: 17px;
  border: 0;
  background: var(--color-primary);
  box-shadow: none;
}

.timeline li {
  grid-template-columns: 24px minmax(0, 1fr);
}

.timeline__card {
  border: 1px solid #dfe8da;
  border-radius: 16px;
  background: #ffffff;
}

.coach-card {
  min-height: auto;
  padding: 22px;
  border-color: #dfe8da;
  background: #ffffff;
}

.speech-bubble {
  max-width: none;
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  color: var(--color-text-strong);
  font-weight: 750;
  line-height: 1.65;
  text-align: left;
}

.speech-bubble::after {
  display: none;
}

.recommend-card {
  border-color: #cfe7ff;
  background: #ffffff;
}

.mini-button {
  padding: 9px 14px;
  border-radius: 999px;
}

.hint {
  padding: 10px 12px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  font-weight: 650;
}

.recommend-card__body {
  gap: 12px;
  margin-top: 14px;
  padding: 18px;
  border: 1px solid #dfe8da;
  border-radius: 18px;
  background: #fbfdf9;
  box-shadow: none;
}

.recommend-card__body h3 {
  font-size: 1.1rem;
  font-weight: 850;
}

.tag-row span {
  padding: 5px 9px;
  background: #ffffff;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 650;
}

.recommend-card__footer {
  border-top: 1px solid #dfe8da;
}

.recommend-card__footer strong {
  color: #176b9f;
  font-size: 1.125rem;
  font-weight: 850;
}

.small-press-button {
  padding: 11px 16px;
  border-radius: 999px;
}

.weekly-card {
  background: #ffffff;
}

.week-bars {
  gap: 8px;
}

.week-bars__bar {
  width: 30px;
  height: 92px;
  padding: 0;
  border: 1px solid #dbe6d5;
  border-radius: 999px;
  background: #f2f6ef;
  box-shadow: none;
  overflow: hidden;
}

.week-bars__bar--today {
  border-color: var(--color-primary);
  background: #f7fbf4;
}

.week-bars__bar b {
  border-radius: 999px;
  box-shadow: none;
}

.status-banner,
.empty-state {
  padding: 13px 15px;
  border: 1px solid #dfe8da;
  border-radius: 16px;
  box-shadow: none;
  font-weight: 700;
}

.status-banner--loading {
  border-color: #b9dfff;
  color: #176b9f;
}

.status-banner--error {
  border-color: #efb7b7;
  background: #fffafa;
}

.status-banner--success {
  border-color: #b9d9ad;
  background: #fbfff8;
}

@media (max-width: 720px) {
  .calorie-ring__track {
    width: 210px;
    height: 210px;
  }
}
</style>
