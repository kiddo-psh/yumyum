<template>
  <div v-if="show" class="mb-8">
    <div class="bg-nyam-mint neo-brutal-border rounded-xl p-4 flex items-center gap-3">
      <span class="material-symbols-outlined text-on-background">campaign</span>
      <button class="flex-1 text-left" @click="open">
        <p class="text-label-lg text-on-background font-bold">{{ latest.weekNumber }}주차 리포트가 발행되었어요!</p>
        <p class="text-label-md text-on-surface-variant">탭하면 이번 주 AI 코칭을 확인할 수 있어요.</p>
      </button>
      <button
        class="w-9 h-9 flex items-center justify-center neo-brutal-border rounded-full bg-white hover:bg-surface transition-colors flex-shrink-0"
        aria-label="알림 닫기"
        @click="dismiss"
      >
        <span class="material-symbols-outlined text-base text-on-background">close</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useWeeklyReports } from '@/composables/useWeeklyReports';

// 백엔드에 읽음 상태가 없으므로 "마지막으로 본 리포트 id"를 localStorage에 둔다.
const STORAGE_KEY = 'nyam.lastSeenWeeklyReportId';

const router = useRouter();
const { latest, load } = useWeeklyReports();
const dismissed = ref(false);

const show = computed(() => {
  if (dismissed.value || !latest.value) return false;
  const seenId = Number(localStorage.getItem(STORAGE_KEY) ?? 0);
  return latest.value.id > seenId;
});

function markSeen() {
  if (latest.value) {
    localStorage.setItem(STORAGE_KEY, String(latest.value.id));
  }
}

function open() {
  markSeen();
  router.push({ name: 'report-detail', params: { weekNumber: latest.value.weekNumber } });
}

function dismiss() {
  markSeen();
  dismissed.value = true;
}

onMounted(load);
</script>
