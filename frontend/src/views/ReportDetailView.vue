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
      <div class="prose-report text-on-background" v-html="renderMd(report.content)"></div>
    </section>

    <!-- 섹션 카드 3개 -->
    <section
      v-for="s in sections"
      :key="s.label"
      class="bg-white neo-brutal-border rounded-2xl p-5"
    >
      <h2 class="text-label-lg text-on-surface-variant mb-2">{{ s.label }}</h2>
      <div class="prose-report text-on-background" v-html="renderMd(s.value)"></div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { marked } from 'marked';
import { getCurrentProgram } from '@/api/my';
import { getWeeklyReport } from '@/api/report';

marked.setOptions({ breaks: true });

function renderMd(content) {
  return marked.parse(content ?? '');
}

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

<style scoped>
/* AI 리포트 마크다운 스타일 */
:deep(.prose-report) {
  font-size: 1rem;
  line-height: 1.7;
}
:deep(.prose-report p) {
  margin: 0 0 0.6em;
}
:deep(.prose-report p:last-child) {
  margin-bottom: 0;
}
:deep(.prose-report strong) {
  font-weight: 800;
}
:deep(.prose-report ul),
:deep(.prose-report ol) {
  margin: 0.4em 0 0.4em 1.2em;
  padding: 0;
}
:deep(.prose-report li) {
  margin-bottom: 0.25em;
}
:deep(.prose-report h1),
:deep(.prose-report h2),
:deep(.prose-report h3) {
  font-weight: 800;
  margin: 0.7em 0 0.35em;
  line-height: 1.3;
}
:deep(.prose-report h1) { font-size: 1.2em; }
:deep(.prose-report h2) { font-size: 1.1em; }
:deep(.prose-report h3) { font-size: 1.02em; }
:deep(.prose-report code) {
  background: #f3f4f6;
  border-radius: 4px;
  padding: 0.1em 0.4em;
  font-size: 0.85em;
  font-family: monospace;
}
:deep(.prose-report pre) {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 0.75em 1em;
  overflow-x: auto;
  margin: 0.5em 0;
}
:deep(.prose-report pre code) {
  background: none;
  padding: 0;
}
</style>
