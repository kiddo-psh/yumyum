<template>
  <RouterLink
    :to="{ name: 'report-detail', params: { weekNumber: report.weekNumber } }"
    class="block bg-surface neo-brutal-border rounded-xl p-5 hover:-translate-y-0.5 transition-all duration-150"
  >
    <div class="flex items-center justify-between">
      <span class="text-headline-md text-on-background font-bold">{{ report.weekNumber }}주차</span>
      <span class="text-label-lg text-on-surface-variant bg-white neo-brutal-border rounded-xl px-3 py-1">
        달성 {{ report.achievementDays }}/7
      </span>
    </div>
    <div class="flex items-center justify-between mt-3">
      <span class="text-label-lg text-on-surface-variant">평균 칼로리 {{ Math.round(report.avgCalorieRate) }}%</span>
      <span class="material-symbols-outlined text-on-surface-variant">chevron_right</span>
    </div>
  </RouterLink>
</template>

<script setup>
defineProps({
  report: { type: Object, required: true },
});
</script>
