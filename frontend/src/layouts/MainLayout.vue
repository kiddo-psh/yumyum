<template>
  <div class="flex min-h-screen bg-background">
    <!-- 모바일 오버레이 -->
    <Transition name="sidebar-overlay">
      <div
        v-if="sidebarOpen && !isLg"
        class="fixed inset-0 bg-black/40 z-30"
        @click="closeSidebar"
      />
    </Transition>

    <!-- SideNavBar -->
    <aside
      id="main-sidebar"
      aria-label="주 내비게이션"
      class="fixed left-0 top-0 h-full w-sidebar-width border-r-[3px] border-on-background bg-sub-background flex flex-col gap-base z-40"
      :style="asideStyle"
    >
      <!-- Brand + 닫기 버튼 -->
      <div class="mb-8 flex items-center gap-3">
        <div class="w-12 h-12 bg-nyam-mint rounded-full flex items-center justify-center neo-brutal-border flex-shrink-0 overflow-hidden">
          <img src="/nyam/nyamnyam.png" alt="냠냠이" class="w-10 h-10 object-contain" />
        </div>
        <div class="flex-1 min-w-0">
          <h1 class="text-display-md font-sans text-on-background tracking-tighter leading-none">
            NyamNyam
          </h1>
          <p class="text-label-lg text-on-surface-variant">
            Coaching Buddy
          </p>
        </div>
        <button
          type="button"
          class="flex-shrink-0 w-9 h-9 rounded-lg flex items-center justify-center text-on-surface-variant hover:bg-surface hover:text-on-background transition-colors neo-brutal-border"
          aria-label="메뉴 닫기"
          @click="closeSidebar"
        >
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
      </div>

      <!-- Nav -->
      <nav class="flex flex-col gap-4">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 text-label-lg"
          :class="[
            isNavActive(item.to)
              ? 'bg-primary text-on-primary neo-brutal-border -translate-y-1'
              : 'text-on-background hover:bg-surface hover:-translate-y-0.5'
          ]"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 획득한 뱃지 -->
      <div class="mt-auto px-4">
        <RouterLink
          v-if="earnedBadges.length"
          to="/my"
          class="flex items-center gap-1 flex-wrap"
          aria-label="뱃지 도감 보기"
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
        <p
          v-if="badgeStore.error"
          class="text-xs text-error"
        >
          뱃지 불러오기 실패
        </p>
      </div>

      <!-- User Profile -->
      <div class="neo-brutal-border bg-white rounded-xl p-4 flex items-center gap-3">
        <div class="w-12 h-12 rounded-full border-2 border-on-background overflow-hidden bg-nyam-mint flex items-center justify-center flex-shrink-0">
          <span
            class="material-symbols-outlined text-on-background"
            style="font-variation-settings:'FILL' 1;"
          >person</span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="text-label-lg leading-none truncate">
            {{ displayName }}
          </p>
          <p class="text-xs text-on-surface-variant">
            {{ goalLabel }}
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

    <!-- 메뉴 열기 버튼 (사이드바 닫혔을 때) -->
    <Transition name="fade">
      <button
        v-if="!sidebarOpen"
        ref="hamburgerRef"
        type="button"
        class="fixed top-5 left-5 z-50 w-11 h-11 bg-surface neo-brutal-border rounded-xl flex items-center justify-center hover:bg-primary hover:text-white transition-colors"
        :aria-expanded="sidebarOpen"
        aria-controls="main-sidebar"
        aria-label="메뉴 열기"
        @click="openSidebar"
      >
        <span class="material-symbols-outlined">menu</span>
      </button>
    </Transition>

    <!-- Main Content -->
    <main
      class="flex-1 min-h-screen"
      :style="mainStyle"
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
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { logout as logoutRequest } from '@/api/auth';
import { clearTokens } from '@/services/auth';
import { useBadgeStore } from '@/stores/badge';
import { useChatStore } from '@/stores/chat';
import { useMemberStore } from '@/stores/member';
import AiChatPanel from '@/components/AiChatPanel.vue';
import BadgeCelebrationOverlay from '@/components/badge/BadgeCelebrationOverlay.vue';
import BadgeImage from '@/components/badge/BadgeImage.vue';

const router = useRouter();
const route = useRoute();

const badgeStore = useBadgeStore();
const chatStore = useChatStore();
const memberStore = useMemberStore();

const LG = 1024
const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const isLg = ref(window.innerWidth >= LG)
const sidebarOpen = ref(isLg.value)

const hamburgerRef = ref(null)

function openSidebar()  { sidebarOpen.value = true }
function closeSidebar() {
  sidebarOpen.value = false
  nextTick(() => hamburgerRef.value?.focus())
}

function handleResize() {
  const lg = window.innerWidth >= LG
  isLg.value = lg
  if (!lg) sidebarOpen.value = false
}

function handleKeydown(e) {
  if (e.key === 'Escape' && sidebarOpen.value) {
    closeSidebar()
    return
  }
  if (e.key === 'b' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    sidebarOpen.value ? closeSidebar() : openSidebar()
  }
}

// 중첩 라우트에서도 상위 경로 활성 상태 유지
function isNavActive(to) {
  if (to === '/') return route.path === '/'
  return route.path.startsWith(to)
}

// 모바일에서 페이지 이동 시 사이드바 자동 닫기
watch(() => route.path, () => {
  if (!isLg.value) closeSidebar()
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
  window.addEventListener('keydown', handleKeydown)
  badgeStore.loadCollection()
  memberStore.loadProfile()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeydown)
})

const SIDEBAR_WIDTH = '280px'
const TRANSITION = prefersReduced ? 'none' : 'transform 300ms cubic-bezier(0.22, 1, 0.36, 1)'
const MAIN_TRANSITION = prefersReduced ? 'none' : 'margin-left 300ms cubic-bezier(0.22, 1, 0.36, 1)'

const asideStyle = computed(() => ({
  padding: '24px',
  transform: sidebarOpen.value ? 'translateX(0)' : 'translateX(-100%)',
  transition: TRANSITION,
}))

const mainStyle = computed(() => ({
  marginLeft: sidebarOpen.value && isLg.value ? SIDEBAR_WIDTH : '0',
  padding: '40px',
  transition: MAIN_TRANSITION,
}))

const PREVIEW_LIMIT = 5;
const earnedBadges = computed(() => badgeStore.earnedBadges);
const previewBadges = computed(() => earnedBadges.value.slice(0, PREVIEW_LIMIT));
const extraBadgeCount = computed(() => Math.max(0, earnedBadges.value.length - PREVIEW_LIMIT));

const GOAL_LABELS = {
  DIET: '다이어트 중',
  MUSCLE: '근육 증가 중',
  HEALTH: '건강 유지 중',
  DISEASE: '질환 관리 중',
}

const SEX_LABELS = { MALE: '남', FEMALE: '여' }

const displayName = computed(() => {
  const p = memberStore.profile
  if (!p) return memberStore.loading ? '로딩 중...' : '냠냠 회원'
  const sexLabel = SEX_LABELS[p.sex] ?? ''
  return p.birthYear ? `${p.birthYear}년생 ${sexLabel}`.trim() : '냠냠 회원'
})

const goalLabel = computed(() => {
  const p = memberStore.profile
  if (!p) return ''
  return GOAL_LABELS[p.healthGoal] ?? p.healthGoal ?? ''
})

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

<style scoped>
.sidebar-overlay-enter-active,
.sidebar-overlay-leave-active {
  transition: opacity 200ms ease;
}
.sidebar-overlay-enter-from,
.sidebar-overlay-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 150ms ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .sidebar-overlay-enter-active,
  .sidebar-overlay-leave-active,
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
