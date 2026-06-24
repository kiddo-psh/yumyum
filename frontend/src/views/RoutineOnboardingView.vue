<template>
  <!-- Header -->
  <header class="mb-8">
    <RouterLink
      to="/routine"
      class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
    >
      <span class="material-symbols-outlined text-base">chevron_left</span>
      <span>운동 루틴으로</span>
    </RouterLink>
    <h1 class="text-display-md text-on-background">
      운동 루틴 만들기
    </h1>
    <p class="text-body-md text-on-surface-variant mt-1">
      몇 가지 질문에 답하면 AI가 맞춤 운동 루틴을 만들어 드립니다.
    </p>
  </header>

  <!-- Step indicator -->
  <ol
    v-if="!result"
    class="flex flex-wrap gap-3 mb-8"
  >
    <li
      v-for="(label, index) in stepLabels"
      :key="label"
      class="neo-brutal-border rounded-full px-4 py-2 text-label-lg"
      :class="stepClass(index + 1)"
    >
      {{ index + 1 }}. {{ label }}
    </li>
  </ol>

  <!-- Step 1: 루틴 보유 여부 -->
  <section
    v-if="!result && step === 1"
    class="bg-white neo-brutal-border rounded-xl p-6"
  >
    <h2 class="text-headline-md text-on-background">
      지금 따라 하는 운동 루틴이 있나요?
    </h2>
    <p class="text-body-md text-on-surface-variant mt-1 mb-6">
      기존 루틴 여부에 따라 AI가 난이도를 조절합니다.
    </p>
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <button
        type="button"
        class="text-left neo-brutal-border rounded-lg p-5 transition-all hover:-translate-y-0.5"
        :class="optionClass(form.hasExistingRoutine === true)"
        @click="selectHasRoutine(true)"
      >
        <strong class="block text-body-lg text-on-background">네, 있어요</strong>
        <small class="text-label-lg text-on-surface-variant">점진적 과부하로 강도를 높여 줘요</small>
      </button>
      <button
        type="button"
        class="text-left neo-brutal-border rounded-lg p-5 transition-all hover:-translate-y-0.5"
        :class="optionClass(form.hasExistingRoutine === false)"
        @click="selectHasRoutine(false)"
      >
        <strong class="block text-body-lg text-on-background">아니요, 처음이에요</strong>
        <small class="text-label-lg text-on-surface-variant">기본기 위주로 가볍게 시작해요</small>
      </button>
    </div>
  </section>

  <!-- Step 2: 주 몇 회 -->
  <section
    v-if="!result && step === 2"
    class="bg-white neo-brutal-border rounded-xl p-6"
  >
    <h2 class="text-headline-md text-on-background">
      일주일에 몇 번 운동할 수 있나요?
    </h2>
    <p class="text-body-md text-on-surface-variant mt-1 mb-6">
      실제로 지킬 수 있는 빈도를 골라 주세요.
    </p>
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
      <button
        v-for="days in dayOptions"
        :key="days"
        type="button"
        class="neo-brutal-border rounded-lg py-5 text-body-lg text-on-background transition-all hover:-translate-y-0.5"
        :class="optionClass(form.daysPerWeek === days)"
        @click="selectDays(days)"
      >
        주 {{ days }}회
      </button>
    </div>
    <div class="flex justify-start mt-6">
      <button
        type="button"
        class="neo-brutal-border rounded-xl bg-white px-5 py-3 text-label-lg text-on-background hover:bg-surface transition-colors"
        @click="step = 1"
      >
        이전
      </button>
    </div>
  </section>

  <!-- Step 3: 분할 선택 -->
  <section
    v-if="!result && step === 3"
    class="bg-white neo-brutal-border rounded-xl p-6"
  >
    <h2 class="text-headline-md text-on-background">
      어떻게 나눠서 운동할까요?
    </h2>
    <p class="text-body-md text-on-surface-variant mt-1 mb-6">
      주 {{ form.daysPerWeek }}회에 맞는 분할 구성입니다.
    </p>

    <div
      v-if="splitState.loading"
      class="flex items-center gap-3 text-on-surface-variant py-6"
    >
      <span class="material-symbols-outlined animate-spin">progress_activity</span>
      <span>분할 옵션을 불러오는 중...</span>
    </div>
    <p
      v-else-if="splitState.error"
      class="text-danger text-body-md font-bold py-4"
    >
      {{ splitState.error }}
    </p>
    <p
      v-else-if="!splitOptions.length"
      class="text-on-surface-variant text-body-md py-4"
    >
      선택 가능한 분할이 없습니다.
    </p>
    <div
      v-else
      class="grid grid-cols-1 sm:grid-cols-2 gap-4"
    >
      <button
        v-for="option in splitOptions"
        :key="option.splitType"
        type="button"
        class="text-left neo-brutal-border rounded-lg p-5 transition-all hover:-translate-y-0.5"
        :class="optionClass(form.splitType === option.splitType)"
        @click="form.splitType = option.splitType"
      >
        <strong class="block text-body-lg text-on-background">{{ option.label }}</strong>
        <small class="text-label-lg text-on-surface-variant">{{ option.splitType }}</small>
      </button>
    </div>

    <p
      v-if="submitState.error"
      class="text-danger text-body-md font-bold mt-4"
    >
      {{ submitState.error }}
    </p>

    <div class="flex justify-between gap-3 mt-6">
      <button
        type="button"
        class="neo-brutal-border rounded-xl bg-white px-5 py-3 text-label-lg text-on-background hover:bg-surface transition-colors"
        @click="step = 2"
      >
        이전
      </button>
      <button
        type="button"
        class="flex items-center gap-2 bg-primary text-on-primary neo-brutal-border rounded-xl px-6 py-3 text-label-lg transition-all hover:-translate-y-0.5 active:translate-y-1 disabled:opacity-40 disabled:hover:translate-y-0"
        :disabled="!form.splitType || submitState.loading"
        @click="handleSubmit"
      >
        <span
          v-if="submitState.loading"
          class="material-symbols-outlined animate-spin"
        >progress_activity</span>
        <span>{{ submitState.loading ? 'AI 루틴 생성 중…' : '루틴 만들기' }}</span>
      </button>
    </div>
  </section>

  <!-- 결과 -->
  <section
    v-if="result"
    class="bg-white neo-brutal-border rounded-xl p-6"
  >
    <h2 class="text-headline-lg text-on-background">
      {{ result.name }}
    </h2>
    <p
      v-if="result.aiComment"
      class="bg-surface neo-brutal-border rounded-lg p-4 text-body-md text-on-background mt-4"
    >
      {{ result.aiComment }}
    </p>

    <p class="text-label-sm text-on-surface-variant mt-5 mb-2">세트·횟수·무게를 조정하려면 바로 수정하세요.</p>

    <div
      v-for="day in groupedDays"
      :key="day.dayLabel"
      class="mt-4"
    >
      <h3 class="text-headline-md text-on-background mb-3">{{ day.dayLabel }}</h3>
      <ul class="grid gap-3">
        <li
          v-for="exercise in day.exercises"
          :key="exercise.id"
          class="neo-brutal-border rounded-xl bg-surface p-4"
        >
          <p class="text-body-md font-bold text-on-background mb-3">{{ exercise.exerciseName }}</p>
          <div class="grid grid-cols-3 gap-2">
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">세트</p>
              <input
                v-model.number="resultEdits[exercise.id].targetSets"
                type="number" min="1" step="1"
                class="w-full neo-brutal-border rounded-lg px-2 py-1.5 text-body-md text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">횟수</p>
              <input
                v-model.number="resultEdits[exercise.id].targetReps"
                type="number" min="1" step="1"
                class="w-full neo-brutal-border rounded-lg px-2 py-1.5 text-body-md text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <p class="text-label-sm text-on-surface-variant mb-1">무게 (kg)</p>
              <input
                v-model.number="resultEdits[exercise.id].targetWeightKg"
                type="number" min="0" step="0.5"
                class="w-full neo-brutal-border rounded-lg px-2 py-1.5 text-body-md text-center bg-white focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>
        </li>
      </ul>
    </div>

    <div class="flex justify-between gap-3 mt-8">
      <button
        type="button"
        class="neo-brutal-border rounded-xl bg-white px-5 py-3 text-label-lg text-on-background hover:bg-surface transition-colors"
        @click="resetOnboarding"
      >
        다시 만들기
      </button>
      <button
        type="button"
        class="flex items-center gap-2 bg-primary text-on-primary neo-brutal-border rounded-xl px-6 py-3 text-label-lg hover:-translate-y-0.5 active:translate-y-1 transition-all disabled:opacity-40"
        :disabled="completing"
        @click="completeOnboarding"
      >
        <span v-if="completing" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
        완료
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { createAiRoutine, getSplitOptions, updateExercise } from '@/api/routine';

const dayOptions = [2, 3, 4, 5];
const stepLabels = ['루틴 보유', '운동 빈도', '분할 선택'];

const step = ref(1);
const splitOptions = ref([]);
const result = ref(null);

const form = reactive({
  hasExistingRoutine: null,
  daysPerWeek: null,
  splitType: null,
});

const splitState = reactive({ loading: false, error: '' });
const submitState = reactive({ loading: false, error: '' });
const resultEdits = reactive({});
const completing = ref(false);
const router = useRouter();

const groupedDays = computed(() => {
  const order = [];
  const byLabel = new Map();

  for (const exercise of result.value?.exercises ?? []) {
    if (!byLabel.has(exercise.dayLabel)) {
      byLabel.set(exercise.dayLabel, []);
      order.push(exercise.dayLabel);
    }
    byLabel.get(exercise.dayLabel).push(exercise);
  }

  return order.map((dayLabel) => ({ dayLabel, exercises: byLabel.get(dayLabel) }));
});

watch(() => form.daysPerWeek, async (days) => {
  form.splitType = null;
  splitOptions.value = [];

  if (!days) {
    return;
  }

  splitState.loading = true;
  splitState.error = '';

  try {
    splitOptions.value = await getSplitOptions(days);
  } catch (error) {
    splitState.error = formatApiError(error, '분할 옵션을 불러오지 못했습니다.');
  } finally {
    splitState.loading = false;
  }
});

function stepClass(index) {
  if (step.value === index) {
    return 'bg-primary text-on-primary';
  }
  if (step.value > index) {
    return 'bg-nyam-mint text-on-background';
  }
  return 'bg-white text-on-surface-variant';
}

function optionClass(selected) {
  return selected
    ? 'bg-nyam-mint -translate-y-0.5'
    : 'bg-white hover:bg-surface';
}

function selectHasRoutine(value) {
  form.hasExistingRoutine = value;
  step.value = 2;
}

function selectDays(days) {
  form.daysPerWeek = days;
  step.value = 3;
}

async function handleSubmit() {
  if (!form.splitType) {
    return;
  }

  submitState.loading = true;
  submitState.error = '';

  try {
    result.value = await createAiRoutine({
      hasExistingRoutine: form.hasExistingRoutine,
      daysPerWeek: form.daysPerWeek,
      splitType: form.splitType,
    });
    for (const ex of result.value.exercises) {
      resultEdits[ex.id] = {
        targetSets: ex.targetSets,
        targetReps: ex.targetReps,
        targetWeightKg: ex.targetWeightKg,
      };
    }
  } catch (error) {
    submitState.error = formatApiError(error, 'AI 루틴 생성에 실패했습니다.');
  } finally {
    submitState.loading = false;
  }
}

async function completeOnboarding() {
  completing.value = true;
  try {
    const routineId = result.value.routineId;
    const changed = result.value.exercises.filter(ex => {
      const e = resultEdits[ex.id];
      return e && (
        e.targetSets !== ex.targetSets ||
        e.targetReps !== ex.targetReps ||
        e.targetWeightKg !== ex.targetWeightKg
      );
    });
    await Promise.all(
      changed.map(ex =>
        updateExercise(routineId, ex.id, {
          exerciseName: ex.exerciseName,
          ...resultEdits[ex.id],
        })
      )
    );
  } catch {
    // 수정 실패해도 루틴은 이미 생성됐으므로 이동
  } finally {
    completing.value = false;
  }
  router.push({ name: 'routine' });
}

function resetOnboarding() {
  result.value = null;
  splitOptions.value = [];
  form.hasExistingRoutine = null;
  form.daysPerWeek = null;
  form.splitType = null;
  step.value = 1;
}

function formatWeight(value) {
  return Number(value ?? 0).toLocaleString('ko-KR');
}

function formatApiError(error, fallbackMessage) {
  if (error?.status === 401) {
    return '로그인이 필요합니다. 다시 로그인해 주세요.';
  }

  return error?.data?.message ?? fallbackMessage;
}
</script>
