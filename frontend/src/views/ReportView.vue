<template>
  <header class="mb-6">
    <h1 class="text-display-md text-on-background">주간 리포트</h1>
    <p class="text-label-lg text-on-surface-variant mt-1">매 7일 AI가 분석한 코칭 리포트예요.</p>
  </header>

  <div v-if="loading" class="text-on-surface-variant text-label-lg">불러오는 중…</div>

  <div v-else-if="noProgram" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background">진행 중인 프로그램이 없어요</p>
  </div>

  <div v-else-if="error" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background mb-3">리포트를 불러오지 못했어요</p>
    <button class="neo-brutal-border rounded-xl bg-nyam-mint/30 px-4 py-2" @click="load">다시 시도</button>
  </div>

  <div v-else-if="reports.length === 0" class="bg-white neo-brutal-border rounded-2xl p-6 text-center">
    <p class="text-headline-sm text-on-background">아직 생성된 주간 리포트가 없어요</p>
    <p class="text-label-lg text-on-surface-variant mt-1">프로그램 시작 7일 후부터 생성돼요.</p>
  </div>

  <ul v-else class="flex flex-col gap-4">
    <li
      v-for="r in reports"
      :key="r.weekNumber"
      class="bg-white neo-brutal-border rounded-2xl p-5 cursor-pointer hover:bg-surface transition-colors"
      @click="goDetail(r.weekNumber)"
    >
      <div class="flex items-center justify-between">
        <span class="text-headline-md text-on-background font-bold">{{ r.weekNumber }}주차</span>
        <span class="text-label-lg text-on-surface-variant neo-brutal-border rounded-full px-3 py-1">
          달성 {{ r.achievementDays }}/7
        </span>
      </div>
      <div class="flex items-center justify-between mt-3">
        <span class="text-label-lg text-on-surface-variant">평균 칼로리 {{ Math.round(r.avgCalorieRate) }}%</span>
        <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
      </div>
    </li>
  </ul>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentProgram } from '@/api/my';
import { getWeeklyReports } from '@/api/report';

const router = useRouter();
const reports = ref([]);
const loading = ref(true);
const error = ref(false);
const noProgram = ref(false);

async function load() {
  loading.value = true;
  error.value = false;
  noProgram.value = false;
  try {
    const program = await getCurrentProgram();
    reports.value = await getWeeklyReports(program.programId);
  } catch (e) {
    if (e?.status === 404) {
      noProgram.value = true;
    } else {
      error.value = true;
    }
  } finally {
    loading.value = false;
  }
}

function goDetail(weekNumber) {
  router.push({ name: 'report-detail', params: { weekNumber } });
}

onMounted(load);
</script>
