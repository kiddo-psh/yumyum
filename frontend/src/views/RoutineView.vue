<template>
  <!-- Header -->
  <header class="flex justify-between items-center mb-10">
    <div>
      <h1 class="text-display-md text-on-background">
        운동 루틴
      </h1>
      <p class="text-body-md text-on-surface-variant mt-1">
        등록된 운동 루틴을 확인하고 새 루틴을 만들 수 있습니다.
      </p>
    </div>
    <RouterLink
      v-if="routines.length"
      to="/routine/onboarding"
      class="flex items-center gap-2 bg-primary text-on-primary neo-brutal-border rounded-xl px-5 py-3 text-label-lg hover:-translate-y-0.5 active:translate-y-1 transition-all"
    >
      <span class="material-symbols-outlined">add</span>
      <span>새 루틴 만들기</span>
    </RouterLink>
  </header>

  <!-- Loading -->
  <div
    v-if="loadState.loading"
    class="flex items-center gap-3 text-on-surface-variant py-6"
  >
    <span class="material-symbols-outlined animate-spin">progress_activity</span>
    <span>루틴을 불러오는 중...</span>
  </div>

  <!-- Error -->
  <div
    v-else-if="loadState.error"
    class="bg-white neo-brutal-border rounded-xl p-6 text-danger text-body-md font-bold"
  >
    {{ loadState.error }}
  </div>

  <!-- 루틴 없음 → 온보딩 유도 -->
  <div
    v-else-if="!routines.length"
    class="bg-white neo-brutal-border rounded-xl flex flex-col items-center text-center gap-4 px-6 py-16"
  >
    <div class="w-20 h-20 bg-nyam-mint rounded-full neo-brutal-border flex items-center justify-center">
      <span
        class="material-symbols-outlined text-on-background text-4xl"
        style="font-variation-settings:'FILL' 1;"
      >fitness_center</span>
    </div>
    <h2 class="text-headline-lg text-on-background">
      아직 운동 루틴이 없어요
    </h2>
    <p class="text-body-md text-on-surface-variant max-w-md">
      몇 가지 질문에 답하면 AI가 나에게 맞는 운동 루틴을 만들어 드립니다.
    </p>
    <RouterLink
      to="/routine/onboarding"
      class="flex items-center gap-2 bg-primary text-on-primary neo-brutal-border rounded-xl px-6 py-3 text-label-lg hover:-translate-y-0.5 active:translate-y-1 transition-all mt-2"
    >
      <span class="material-symbols-outlined">add</span>
      <span>운동 루틴 만들기</span>
    </RouterLink>
  </div>

  <!-- 루틴 목록 -->
  <ul
    v-else
    class="grid grid-cols-1 md:grid-cols-2 gap-6"
  >
    <li v-for="routine in routines" :key="routine.routineId">
      <RouterLink
        :to="{ name: 'routine-detail', params: { routineId: routine.routineId } }"
        class="bg-white neo-brutal-border rounded-xl p-6 flex items-center justify-between gap-4 neo-brutal-card-hover block"
      >
        <div class="min-w-0">
          <h2 class="text-headline-md text-on-background truncate">
            {{ routine.name }}
          </h2>
          <p class="text-body-md text-on-surface-variant mt-1">
            주 {{ routine.daysPerWeek }}회
          </p>
        </div>
        <div class="flex items-center gap-3 shrink-0">
          <span
            class="neo-brutal-border rounded-full px-3 py-1.5 text-label-lg"
            :class="routine.aiGenerated ? 'bg-nyam-mint text-on-background' : 'bg-surface text-on-surface-variant'"
          >
            {{ routine.aiGenerated ? 'AI 생성' : '직접 등록' }}
          </span>
          <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
        </div>
      </RouterLink>
    </li>
  </ul>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';

import { getMyRoutines } from '@/api/routine';

const routines = ref([]);
const loadState = reactive({ loading: false, error: '' });

onMounted(loadRoutines);

async function loadRoutines() {
  loadState.loading = true;
  loadState.error = '';

  try {
    routines.value = await getMyRoutines();
  } catch (error) {
    loadState.error = formatApiError(error, '루틴 목록을 불러오지 못했습니다.');
    routines.value = [];
  } finally {
    loadState.loading = false;
  }
}

function formatApiError(error, fallbackMessage) {
  if (error?.status === 401) {
    return '로그인이 필요합니다. 다시 로그인해 주세요.';
  }

  return error?.data?.message ?? fallbackMessage;
}
</script>
