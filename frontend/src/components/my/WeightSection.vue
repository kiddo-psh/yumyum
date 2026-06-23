<template>
  <section class="neo-brutal-border rounded-xl bg-surface p-8">
    <h2 class="text-headline-lg text-on-background mb-6">체중 기록</h2>

    <p v-if="error" class="text-on-surface-variant">체중 기록을 불러올 수 없어요</p>

    <template v-else>
      <!-- 입력 폼 -->
      <form class="flex gap-3 mb-6" @submit.prevent="submit">
        <input v-model.number="newWeight" type="number" step="0.1" placeholder="kg"
               class="flex-1 neo-brutal-border rounded-lg px-3 py-2 text-body-md" required />
        <input v-model="newDate" type="date"
               class="neo-brutal-border rounded-lg px-3 py-2 text-body-md" required />
        <button type="submit"
                class="px-5 py-2 bg-primary text-on-primary neo-brutal-border rounded-lg text-label-lg"
                :disabled="submitting">
          {{ submitting ? '저장 중' : '기록' }}
        </button>
      </form>

      <!-- 목록 -->
      <div v-if="weights.length === 0" class="text-center py-6 text-on-surface-variant">
        <span class="material-symbols-outlined text-4xl mb-2">monitor_weight</span>
        <p class="text-body-md">첫 체중을 기록해보세요</p>
      </div>

      <ul v-else class="flex flex-col gap-3">
        <li v-for="w in recentWeights" :key="w.id"
            class="flex items-center justify-between neo-brutal-border rounded-lg px-4 py-3 bg-white">
          <span class="text-body-md text-on-background">{{ w.recordedDate }}</span>
          <span class="text-headline-lg text-on-background font-bold">{{ w.weightKg }} kg</span>
        </li>
      </ul>
    </template>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue';
import { apiClient } from '@/services/apiClient';

const props = defineProps({
  initialWeights: { type: Array, default: () => [] },
  error: { type: Object, default: null },
});

const weights = ref([...props.initialWeights]);
const newWeight = ref(null);
const newDate = ref(new Date().toISOString().slice(0, 10));
const submitting = ref(false);

const recentWeights = computed(() => weights.value.slice(0, 5));

async function submit() {
  submitting.value = true;
  try {
    const created = await apiClient.post('/weights', {
      weightKg: newWeight.value,
      recordedDate: newDate.value,
    });
    weights.value.unshift(created);
    newWeight.value = null;
  } finally {
    submitting.value = false;
  }
}
</script>
