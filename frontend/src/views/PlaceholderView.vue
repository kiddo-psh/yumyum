<template>
  <section class="placeholder-view">
    <RouterLink
      to="/"
      class="placeholder-view__back"
    >
      대시보드로
    </RouterLink>
    <h1>{{ heading }}</h1>
    <p>{{ description }}</p>
    <RouterLink
      class="placeholder-view__button"
      to="/meals/manual"
    >
      Meal 직접 입력하기
    </RouterLink>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const heading = computed(() => route.meta.heading ?? route.meta.title ?? 'YumYum');
const description = computed(() => route.meta.description ?? '요구된 화면으로 전환되었습니다.');
</script>

<style scoped>
.placeholder-view {
  display: grid;
  max-width: 720px;
  gap: 16px;
  padding: 32px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-2xl);
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.placeholder-view__back {
  width: fit-content;
  color: var(--color-success);
  font-weight: 800;
  text-decoration: none;
}

.placeholder-view h1 {
  margin: 0;
  color: var(--color-text-strong);
}

.placeholder-view p {
  margin: 0;
  color: var(--color-text-muted);
}

.placeholder-view__button {
  width: fit-content;
  margin-top: 8px;
  padding: 12px 18px;
  border: 2px solid #1e5000;
  border-radius: var(--radius-lg);
  background: var(--color-primary);
  box-shadow: var(--shadow-press-green);
  color: #1e5000;
  font-weight: 800;
  text-decoration: none;
}

.placeholder-view__button:active {
  transform: translateY(3px);
  box-shadow: none;
}
</style>
