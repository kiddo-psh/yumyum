<template>
  <section class="neo-brutal-border rounded-xl bg-white p-8">
    <div class="flex items-center justify-between mb-7">
      <h2 class="text-headline-lg font-bold text-on-background">업적</h2>
      <div v-if="store.loaded" class="flex items-baseline gap-1 px-4 py-2 bg-surface neo-brutal-border rounded-xl">
        <span class="text-headline-md font-bold text-primary">{{ store.earnedBadges.length }}</span>
        <span class="text-label-lg text-on-surface-variant"> / {{ store.badges.length }} 획득</span>
      </div>
    </div>

    <!-- 에러 -->
    <p v-if="store.error" class="text-body-md text-on-surface-variant">
      뱃지를 불러올 수 없어요
    </p>

    <!-- 로딩 -->
    <p v-else-if="!store.loaded" class="text-body-md text-on-surface-variant">불러오는 중...</p>

    <!-- 데이터 -->
    <div v-else class="grid grid-cols-4 sm:grid-cols-6 lg:grid-cols-8 gap-4">
      <div
        v-for="badge in store.badges"
        :key="badge.code"
        class="group relative flex items-center justify-center"
      >
        <BadgeImage
          :code="badge.code"
          :alt="badge.name"
          :locked="!badge.earned"
          class="w-full aspect-square transition-transform duration-150 group-hover:-translate-y-1"
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
          class="pointer-events-none absolute bottom-full left-1/2 mb-2 w-44 -translate-x-1/2 z-10 opacity-0 group-hover:opacity-100 transition-opacity duration-150 bg-white neo-brutal-border rounded-xl p-4 text-center"
        >
          <p
            class="text-body-md font-bold"
            :class="badge.earned ? 'text-on-background' : 'text-on-surface-variant'"
          >{{ badge.name }}</p>
          <p class="text-label-lg text-on-surface-variant leading-snug mt-1">{{ badge.description }}</p>
          <p
            class="text-label-lg font-bold mt-2"
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
