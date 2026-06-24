<template>
  <section class="neo-brutal-border rounded-xl bg-surface p-8 neo-brutal-card-hover">
    <div class="flex items-end justify-between mb-6">
      <h2 class="text-headline-lg text-on-background">업적</h2>
      <p v-if="store.loaded" class="text-label-lg text-on-surface-variant">
        {{ store.earnedBadges.length }} / {{ store.badges.length }} 획득
      </p>
    </div>

    <!-- 에러 -->
    <p v-if="store.error" class="text-on-surface-variant">
      뱃지를 불러올 수 없어요
    </p>

    <!-- 로딩 -->
    <p v-else-if="!store.loaded" class="text-on-surface-variant">불러오는 중...</p>

    <!-- 데이터 (구분 없이 순서대로) -->
    <div v-else class="grid grid-cols-6 sm:grid-cols-8 lg:grid-cols-10 gap-3">
      <div
        v-for="badge in store.badges"
        :key="badge.code"
        class="group relative flex items-center justify-center"
      >
        <BadgeImage
          :code="badge.code"
          :alt="badge.name"
          :locked="!badge.earned"
          class="w-full aspect-square transition-transform group-hover:-translate-y-1"
          :class="{ 'blur-sm': !badge.earned }"
        />

        <!-- 미획득 잠금 표시 -->
        <span
          v-if="!badge.earned"
          class="material-symbols-outlined absolute inset-0 m-auto h-fit w-fit text-on-surface-variant"
          style="font-variation-settings:'FILL' 1;"
        >lock</span>

        <!-- hover 정보 -->
        <div
          class="pointer-events-none absolute bottom-full left-1/2 mb-2 w-44 -translate-x-1/2 z-10 opacity-0 group-hover:opacity-100 transition-opacity bg-white neo-brutal-border rounded-lg p-3 text-center"
        >
          <p
            class="text-body-md font-bold"
            :class="badge.earned ? 'text-on-background' : 'text-on-surface-variant'"
          >{{ badge.name }}</p>
          <p class="text-label-lg text-on-surface-variant leading-snug mt-1">{{ badge.description }}</p>
          <p
            class="text-[10px] mt-1"
            :class="badge.earned ? 'text-primary' : 'text-on-surface-variant'"
          >{{ badge.earned ? formatEarnedAt(badge.earnedAt) : '아직 획득 전' }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue';

import BadgeImage from '@/components/badge/BadgeImage.vue';
import { useBadgeStore } from '@/stores/badge';

const store = useBadgeStore();

function formatEarnedAt(value) {
  if (!value) return '';
  // earnedAt은 타임존 없는 LocalDateTime (예: 2026-06-20T21:13:00)
  return `${value.slice(0, 10)} 획득`;
}

onMounted(() => {
  store.loadCollection();
});
</script>
