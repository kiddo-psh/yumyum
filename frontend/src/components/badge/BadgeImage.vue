<template>
  <img
    :src="src"
    :alt="alt"
    class="object-contain"
    :class="{ 'grayscale opacity-40': locked }"
    loading="lazy"
    draggable="false"
  >
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  // Badge enum 식별자 (예: VEGGIE_MANIA). 이미지 파일명의 기준이 된다.
  code: { type: String, required: true },
  alt: { type: String, default: '' },
  // 미획득 뱃지는 회색 처리한다.
  locked: { type: Boolean, default: false },
});

// 뱃지 이미지는 public/badges/<CODE>.png 에 둔다. (에셋은 추후 추가)
const src = computed(() => `${import.meta.env.BASE_URL}badges/${props.code}.png`);
</script>
