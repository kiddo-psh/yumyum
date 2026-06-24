<template>
  <div class="flex min-h-screen bg-background">
    <!-- SideNavBar -->
    <aside
      class="fixed left-0 top-0 h-full w-sidebar-width border-r-[3px] border-on-background bg-sub-background flex flex-col gap-base z-40"
      style="padding: 24px;"
    >
      <!-- Brand -->
      <div class="mb-8 flex items-center gap-3">
        <div class="w-12 h-12 bg-nyam-mint rounded-full flex items-center justify-center neo-brutal-border flex-shrink-0 overflow-hidden">
          <img src="/nyam/nyamnyam.png" alt="냠냠이" class="w-10 h-10 object-contain" />
        </div>
        <div>
          <h1 class="text-display-md font-sans text-on-background tracking-tighter leading-none">
            NyamNyam
          </h1>
          <p class="text-label-lg text-on-surface-variant">
            Coaching Buddy
          </p>
        </div>
      </div>

      <!-- Nav -->
      <nav class="flex flex-col gap-4">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 text-label-lg"
          :class="[
            $route.path === item.to
              ? 'bg-primary text-on-primary neo-brutal-border -translate-y-1'
              : 'text-on-background hover:bg-surface hover:-translate-y-0.5'
          ]"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 획득한 뱃지 (테두리 없이 이미지만) -->
      <RouterLink
        v-if="earnedBadges.length"
        to="/my"
        class="mt-auto flex items-center gap-1 flex-wrap px-4"
        title="뱃지 도감 보기"
      >
        <BadgeImage
          v-for="badge in previewBadges"
          :key="badge.code"
          :code="badge.code"
          :alt="badge.name"
          class="w-9 h-9 hover:-translate-y-0.5 transition-transform"
        />
        <span
          v-if="extraBadgeCount > 0"
          class="text-label-lg text-on-surface-variant"
        >+{{ extraBadgeCount }}</span>
      </RouterLink>

      <!-- User Profile -->
      <div class="neo-brutal-border bg-white rounded-xl p-4 flex items-center gap-3"
           :class="{ 'mt-auto': !earnedBadges.length }">
        <div class="w-12 h-12 rounded-full border-2 border-on-background overflow-hidden bg-nyam-mint flex items-center justify-center flex-shrink-0">
          <span
            class="material-symbols-outlined text-on-background"
            style="font-variation-settings:'FILL' 1;"
          >person</span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="text-label-lg leading-none truncate">
            냠냠마스터
          </p>
          <p class="text-[10px] text-on-surface-variant">
            Lv.12 훈련사
          </p>
        </div>
        <button
          type="button"
          class="flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center text-on-surface-variant hover:bg-surface hover:text-on-background transition-colors"
          aria-label="로그아웃"
          title="로그아웃"
          @click="onLogout"
        >
          <span class="material-symbols-outlined">logout</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main
      class="ml-sidebar-width flex-1 min-h-screen"
      style="padding: 40px; max-width: calc(1440px - 280px);"
    >
      <RouterView />
    </main>

    <!-- AI 채팅 패널 -->
    <AiChatPanel />

    <!-- Floating Action Button — AI 영양 상담 -->
    <button
      class="fixed bottom-10 right-10 w-20 h-20 bg-primary neo-brutal-border rounded-full flex items-center justify-center neo-brutal-shadow hover:scale-110 active:translate-y-2 transition-all z-50 text-white"
      :aria-label="chatStore.isOpen ? '채팅 닫기' : 'AI 영양 상담'"
      @click="chatStore.toggle()"
    >
      <span
        class="material-symbols-outlined text-4xl"
        style="font-variation-settings:'FILL' 1;"
      >{{ chatStore.isOpen ? 'close' : 'psychology' }}</span>
    </button>

    <!-- 뱃지/스트릭 획득 연출 (기록 응답 piggyback) -->
    <BadgeCelebrationOverlay />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';

import { logout as logoutRequest } from '@/api/auth';
import { clearTokens } from '@/services/auth';
import { useBadgeStore } from '@/stores/badge';
import { useChatStore } from '@/stores/chat';
import AiChatPanel from '@/components/AiChatPanel.vue';
import BadgeCelebrationOverlay from '@/components/badge/BadgeCelebrationOverlay.vue';
import BadgeImage from '@/components/badge/BadgeImage.vue';

const router = useRouter();

const badgeStore = useBadgeStore();
const chatStore = useChatStore();

const PREVIEW_LIMIT = 5;
const earnedBadges = computed(() => badgeStore.earnedBadges);
const previewBadges = computed(() => earnedBadges.value.slice(0, PREVIEW_LIMIT));
const extraBadgeCount = computed(() => Math.max(0, earnedBadges.value.length - PREVIEW_LIMIT));

onMounted(() => {
  badgeStore.loadCollection();
});

const navItems = [
  { to: '/',        label: '홈',        icon: 'home' },
  { to: '/log',     label: '식단 기록',  icon: 'restaurant_menu' },
  { to: '/routine', label: '운동 루틴',  icon: 'fitness_center' },
  { to: '/dashboard', label: '대시보드',  icon: 'dashboard' },
  { to: '/my',      label: '마이페이지', icon: 'person' },
]

async function onLogout() {
  try {
    await logoutRequest();
  } catch {
    // 서버 로그아웃 실패와 무관하게 로컬 세션은 정리한다.
  } finally {
    clearTokens();
    router.push({ name: 'login' });
  }
}
</script>
