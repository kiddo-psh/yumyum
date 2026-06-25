<template>
  <section>
    <h2 class="text-headline-md text-on-background font-bold mb-4">주간 리포트</h2>

    <div v-if="loading" class="text-on-surface-variant text-label-lg">불러오는 중…</div>

    <div v-else-if="noProgram" class="bg-white neo-brutal-border rounded-2xl p-5 text-center">
      <p class="text-label-lg text-on-surface-variant">진행 중인 프로그램이 없어요</p>
    </div>

    <div v-else-if="error" class="bg-white neo-brutal-border rounded-2xl p-5 text-center">
      <p class="text-label-lg text-on-surface-variant mb-3">리포트를 불러오지 못했어요</p>
      <button class="neo-brutal-border rounded-xl bg-nyam-mint/30 px-4 py-2 text-label-lg" @click="load">다시 시도</button>
    </div>

    <div v-else-if="reports.length === 0" class="bg-white neo-brutal-border rounded-2xl p-5 text-center">
      <p class="text-label-lg text-on-surface-variant">아직 생성된 주간 리포트가 없어요</p>
    </div>

    <ul v-else class="flex flex-col gap-3">
      <li v-for="r in reports" :key="r.weekNumber">
        <WeeklyReportCard :report="r" />
      </li>
    </ul>
  </section>
</template>

<script setup>
import { onMounted } from 'vue';
import WeeklyReportCard from '@/components/WeeklyReportCard.vue';
import { useWeeklyReports } from '@/composables/useWeeklyReports';

const { reports, loading, error, noProgram, load } = useWeeklyReports();

onMounted(load);
</script>
