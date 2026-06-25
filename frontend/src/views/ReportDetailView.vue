<template>
  <header class="mb-6">
    <RouterLink
      to="/report"
      class="inline-flex items-center gap-1 text-label-lg text-on-surface-variant hover:text-on-background transition-colors mb-3"
    >
      <span class="material-symbols-outlined text-base">chevron_left</span>
      <span>리포트 목록으로</span>
    </RouterLink>
    <h1 class="text-display-md text-on-background">{{ weekNumber }}주차 리포트</h1>
  </header>

  <div v-if="loading" class="text-on-surface-variant text-label-lg">불러오는 중…</div>

  <div v-else-if="notFound" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background">해당 주차 리포트가 없어요</p>
  </div>

  <div v-else-if="error" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background mb-3">리포트를 불러오지 못했어요</p>
    <button class="neo-brutal-border rounded-xl bg-nyam-mint/30 px-4 py-2" @click="load">다시 시도</button>
  </div>

  <div v-else-if="report" class="flex flex-col gap-4">
    <!-- 통계 카드 -->
    <div class="bg-white neo-brutal-border rounded-2xl p-5 grid grid-cols-3 gap-2 text-center">
      <div>
        <p class="text-headline-md text-on-background font-bold">{{ Math.round(report.avgCalorieRate) }}%</p>
        <p class="text-label-md text-on-surface-variant mt-1">평균 칼로리</p>
      </div>
      <div>
        <p class="text-headline-md text-on-background font-bold">{{ report.achievementDays }}/7</p>
        <p class="text-label-md text-on-surface-variant mt-1">목표 달성일</p>
      </div>
      <div>
        <p class="text-headline-md text-on-background font-bold">{{ weightTrendText }}</p>
        <p class="text-label-md text-on-surface-variant mt-1">체중 추세</p>
      </div>
    </div>

    <!-- 종합 코멘트 -->
    <section class="bg-white neo-brutal-border rounded-2xl p-5">
      <h2 class="text-label-lg text-on-surface-variant mb-2">💬 종합 코멘트</h2>
      <p class="text-body-lg text-on-background whitespace-pre-line">{{ report.content }}</p>
    </section>

    <!-- 섹션 카드 3개 -->
    <section
      v-for="s in sections"
      :key="s.label"
      class="bg-white neo-brutal-border rounded-2xl p-5"
    >
      <h2 class="text-label-lg text-on-surface-variant mb-2">{{ s.label }}</h2>
      <p class="text-body-lg text-on-background whitespace-pre-line">{{ s.value }}</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { getCurrentProgram } from '@/api/my';
import { getWeeklyReport } from '@/api/report';

const route = useRoute();
const weekNumber = Number(route.params.weekNumber);

const report = ref(null);
const loading = ref(true);
const error = ref(false);
const notFound = ref(false);

const weightTrendText = computed(() => {
  const t = report.value?.weightTrend;
  if (t === null || t === undefined) return '기록 없음';
  const sign = t > 0 ? '+' : '';
  return `${sign}${t.toFixed(1)}kg`;
});

const sections = computed(() => report.value ? [
  { label: '🥗 영양', value: report.value.nutritionSummary },
  { label: '🏋️ 운동', value: report.value.exerciseSummary },
  { label: '🎯 목표', value: report.value.goalSummary },
] : []);

async function load() {
  loading.value = true;
  error.value = false;
  notFound.value = false;
  try {
    const program = await getCurrentProgram();
    report.value = await getWeeklyReport(program.programId, weekNumber);
  } catch (e) {
    if (e?.status === 404) {
      notFound.value = true;
    } else {
      error.value = true;
    }
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>
