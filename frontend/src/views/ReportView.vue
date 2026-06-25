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
    <li v-for="r in reports" :key="r.weekNumber">
      <WeeklyReportCard :report="r" />
    </li>
  </ul>
</template>

<script setup>
import { onMounted } from 'vue';
import WeeklyReportCard from '@/components/WeeklyReportCard.vue';
import { useWeeklyReports } from '@/composables/useWeeklyReports';

const { reports, loading, error, noProgram, load } = useWeeklyReports();

onMounted(load);
</script>
