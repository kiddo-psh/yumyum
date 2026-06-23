<template>
  <main class="callback">
    <div
      class="callback__spinner"
      aria-hidden="true"
    />
    <p class="callback__text">
      {{ message }}
    </p>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { storeTokens } from '@/services/auth';

const route = useRoute();
const router = useRouter();
const message = ref('로그인 중이에요...');

onMounted(() => {
  const accessToken = typeof route.query.accessToken === 'string' ? route.query.accessToken : null;
  const refreshToken = typeof route.query.refreshToken === 'string' ? route.query.refreshToken : null;
  const needsOnboarding = route.query.needsOnboarding === 'true';

  if (!accessToken) {
    message.value = '로그인에 실패했어요. 다시 시도해 주세요.';
    router.replace({ name: 'login' });
    return;
  }

  storeTokens({ accessToken, refreshToken });

  // 온보딩이 필요한 신규 회원은 루틴 온보딩으로, 기존 회원은 홈으로.
  router.replace(needsOnboarding ? { name: 'routine-onboarding' } : { name: 'home' });
});
</script>

<style scoped>
.callback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  min-height: 100vh;
  background: var(--color-nyam-mint, #a8e6cf);
}

.callback__spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #2d2d2d;
  border-top-color: transparent;
  border-radius: 9999px;
  animation: callback-spin 0.8s linear infinite;
}

.callback__text {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #2d2d2d;
}

@keyframes callback-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .callback__spinner {
    animation-duration: 2s;
  }
}
</style>
