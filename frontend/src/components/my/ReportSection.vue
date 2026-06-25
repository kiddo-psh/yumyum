<template>
  <section class="neo-brutal-border rounded-xl bg-white p-8">
    <div class="flex items-center justify-between mb-7">
      <h2 class="text-headline-lg font-bold text-on-background">주간 리포트</h2>
      <span
        v-if="!loading && !error && !noProgram"
        class="flex items-baseline gap-1 px-4 py-2 bg-surface neo-brutal-border rounded-xl"
      >
        <span class="text-headline-md font-bold text-primary">{{ reports.length }}</span>
        <span class="text-label-lg text-on-surface-variant">개</span>
      </span>
    </div>

    <p v-if="loading" class="text-body-md text-on-surface-variant">불러오는 중...</p>

    <div v-else-if="noProgram" class="text-center py-12 text-on-surface-variant">
      <span class="material-symbols-outlined text-5xl mb-3 block">summarize</span>
      <p class="text-body-lg">진행 중인 Program이 없어요</p>
    </div>

    <div v-else-if="error" class="text-center py-12 text-on-surface-variant">
      <span class="material-symbols-outlined text-5xl mb-3 block">error</span>
      <p class="text-body-lg mb-5">리포트를 불러올 수 없어요</p>
      <button
        class="px-5 py-2.5 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-transform"
        @click="load"
      >
        다시 시도
      </button>
    </div>

    <div v-else-if="reports.length === 0" class="text-center py-12 text-on-surface-variant">
      <span class="material-symbols-outlined text-5xl mb-3 block">edit_calendar</span>
      <p class="text-body-lg">아직 생성된 주간 리포트가 없어요</p>
    </div>

    <ul v-else class="flex flex-col gap-4">
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
