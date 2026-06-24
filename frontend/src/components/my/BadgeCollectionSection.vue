<template>
  <section class="neo-brutal-border rounded-xl bg-surface p-8 neo-brutal-card-hover">
    <div class="flex items-end justify-between mb-6">
      <h2 class="text-headline-lg text-on-background">뱃지 도감</h2>
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

    <!-- 데이터 -->
    <div v-else class="flex flex-col gap-8">
      <div v-for="group in groups" :key="group.category">
        <h3 class="text-label-lg text-on-surface-variant mb-3">{{ group.label }}</h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          <article
            v-for="badge in group.badges"
            :key="badge.code"
            class="neo-brutal-border rounded-xl p-4 flex flex-col items-center text-center gap-2 transition-transform"
            :class="badge.earned ? 'bg-white hover:-translate-y-1' : 'bg-gray-100'"
          >
            <span
              class="text-4xl leading-none"
              :class="{ 'grayscale opacity-40': !badge.earned }"
            >{{ badge.icon }}</span>
            <p
              class="text-body-md font-bold"
              :class="badge.earned ? 'text-on-background' : 'text-on-surface-variant'"
            >{{ badge.name }}</p>
            <p class="text-label-lg text-on-surface-variant leading-snug">{{ badge.description }}</p>
            <p v-if="badge.earned" class="text-[10px] text-primary mt-auto">
              {{ formatEarnedAt(badge.earnedAt) }}
            </p>
            <span
              v-else
              class="material-symbols-outlined text-base text-on-surface-variant mt-auto"
            >lock</span>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted } from 'vue';

import { useBadgeStore } from '@/stores/badge';

const store = useBadgeStore();

// 컬렉션 화면 그룹핑 순서. category enum 값은 백엔드 기준(DIET/WORKOUT/STREAK).
const CATEGORY_ORDER = [
  { category: 'DIET', label: '식단' },
  { category: 'WORKOUT', label: '운동' },
  { category: 'STREAK', label: '연속 달성' },
];

const groups = computed(() =>
  CATEGORY_ORDER
    .map(({ category, label }) => ({
      category,
      label,
      badges: store.badges.filter((b) => b.category === category),
    }))
    .filter((group) => group.badges.length > 0),
);

function formatEarnedAt(value) {
  if (!value) return '';
  // earnedAt은 타임존 없는 LocalDateTime (예: 2026-06-20T21:13:00)
  return `${value.slice(0, 10)} 획득`;
}

onMounted(() => {
  store.loadCollection();
});
</script>
