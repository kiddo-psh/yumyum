<template>
  <Transition name="celebrate">
    <div
      v-if="item"
      class="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-6"
      @click.self="dismiss"
    >
      <div class="w-full max-w-sm bg-white neo-brutal-border neo-brutal-shadow rounded-2xl p-8 text-center">
        <!-- 스트릭 갱신 -->
        <template v-if="item.kind === 'streak'">
          <span class="text-6xl leading-none block mb-4 animate-bounce">🔥</span>
          <p class="text-label-lg text-primary mb-1">스트릭 갱신!</p>
          <p class="text-display-md text-on-background leading-tight">{{ item.current }}일 연속</p>
          <p class="text-body-md text-on-surface-variant mt-2">오늘도 목표를 달성했어요</p>
        </template>

        <!-- 뱃지 획득 -->
        <template v-else>
          <BadgeImage
            :code="item.badge.code"
            :alt="item.badge.name"
            class="w-24 h-24 mx-auto mb-4 animate-bounce"
          />
          <p class="text-label-lg text-primary mb-1">뱃지 획득!</p>
          <p class="text-headline-lg text-on-background leading-tight">{{ item.badge.name }}</p>
          <p class="text-body-md text-on-surface-variant mt-2 leading-relaxed">{{ item.badge.description }}</p>
        </template>

        <button
          type="button"
          class="mt-6 w-full py-3 bg-primary text-white neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-1 transition-transform"
          @click="dismiss"
        >
          {{ hasMore ? '다음' : '확인' }}
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue';
import { storeToRefs } from 'pinia';

import { useBadgeStore } from '@/stores/badge';
import BadgeImage from '@/components/badge/BadgeImage.vue';

const store = useBadgeStore();
const { current: item, queue } = storeToRefs(store);

const hasMore = computed(() => queue.value.length > 0);

function dismiss() {
  store.dismiss();
}
</script>

<style scoped>
.celebrate-enter-active,
.celebrate-leave-active {
  transition: opacity 0.2s ease;
}
.celebrate-enter-from,
.celebrate-leave-to {
  opacity: 0;
}
</style>
