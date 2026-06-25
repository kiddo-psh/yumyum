<template>
  <section class="neo-brutal-border rounded-xl p-8 neo-brutal-card-hover" :class="bgClass">

    <!-- 에러 -->
    <template v-if="error">
      <div class="flex items-center gap-3 text-on-surface-variant">
        <span class="material-symbols-outlined text-3xl">error</span>
        <p class="text-body-md">냠냠이 상태를 불러올 수 없어요</p>
      </div>
    </template>

    <!-- 로딩 -->
    <template v-else-if="!nyam">
      <div class="flex items-center gap-3 text-on-surface-variant">
        <span class="material-symbols-outlined text-3xl animate-spin">progress_activity</span>
        <p class="text-body-md">불러오는 중...</p>
      </div>
    </template>

    <!-- 데이터 -->
    <template v-else>
      <div class="flex items-center gap-8">

        <!-- 냠냠이 -->
        <div class="shrink-0" :class="iconAnimationClass">
          <img src="/nyam/nyamnyam.png" alt="냠냠이" class="w-36 h-36 object-contain" />
        </div>

        <!-- 정보 -->
        <div class="flex-1 min-w-0">
          <p class="text-label-lg text-on-surface-variant">{{ healthGoalLabel }} 중</p>
          <p class="text-display-md font-bold text-on-background mt-1 leading-tight">냠냠이</p>
          <p class="text-headline-md text-on-background mt-4 leading-snug">{{ nyam.message }}</p>
          <div class="flex items-baseline gap-1.5 mt-5">
            <span class="text-numeral-xl text-primary leading-none">{{ Math.round(nyam.achievementRate * 100) }}</span>
            <span class="text-headline-md text-on-surface-variant">%</span>
            <span class="text-body-md text-on-surface-variant ml-1">오늘 달성률</span>
          </div>
        </div>

      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  nyam: { type: Object, default: null },
  error: { type: Object, default: null },
});

const HEALTH_GOAL_LABELS = {
  DIET: '다이어트',
  MUSCLE: '근육 증가',
  HEALTH: '건강 유지',
  DISEASE: '질환 관리',
};

const bgClass = computed(() => {
  if (!props.nyam) return 'bg-surface';
  return { HAPPY: 'bg-nyam-mint', NORMAL: 'bg-yellow-200', SAD: 'bg-gray-200' }[props.nyam.mood] ?? 'bg-surface';
});

const iconAnimationClass = computed(() => {
  if (!props.nyam) return '';
  return { HAPPY: 'animate-bounce', NORMAL: '', SAD: 'animate-pulse' }[props.nyam.mood] ?? '';
});

const healthGoalLabel = computed(() =>
  props.nyam ? (HEALTH_GOAL_LABELS[props.nyam.healthGoal] ?? props.nyam.healthGoal) : ''
);
</script>
