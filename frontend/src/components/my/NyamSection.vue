<template>
  <section class="neo-brutal-border rounded-xl p-8 flex items-center gap-8 neo-brutal-card-hover"
           :class="bgClass">
    <!-- 에러 -->
    <template v-if="error">
      <span class="material-symbols-outlined text-4xl text-on-surface-variant">error</span>
      <p class="text-label-lg text-on-surface-variant">냠냠이 상태를 불러올 수 없어요</p>
    </template>

    <!-- 로딩 -->
    <template v-else-if="!nyam">
      <span class="material-symbols-outlined text-5xl animate-spin text-on-surface-variant">progress_activity</span>
    </template>

    <!-- 데이터 -->
    <template v-else>
      <div class="w-24 h-24 neo-brutal-border rounded-full flex items-center justify-center bg-white flex-shrink-0"
           :class="iconAnimationClass">
        <span class="material-symbols-outlined text-5xl text-on-background"
              style="font-variation-settings:'FILL' 1;">pets</span>
      </div>

      <div>
        <p class="text-display-md font-sans text-on-background leading-tight">냠냠이</p>
        <p class="text-body-md text-on-surface-variant mt-1">{{ healthGoalLabel }} 중</p>
        <p class="text-headline-lg text-on-background mt-3">{{ nyam.message }}</p>
        <p class="text-label-lg text-on-surface-variant mt-2">
          오늘 달성률: {{ Math.round(nyam.achievementRate * 100) }}%
        </p>
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
