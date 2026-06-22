<template>
  <div class="flex min-h-screen bg-background">
    <!-- SideNavBar -->
    <aside class="fixed left-0 top-0 h-full w-sidebar-width border-r-[3px] border-on-background bg-sub-background flex flex-col gap-base z-40" style="padding: 24px;">
      <!-- Brand -->
      <div class="mb-8 flex items-center gap-3">
        <div class="w-12 h-12 bg-nyam-mint rounded-full flex items-center justify-center neo-brutal-border flex-shrink-0">
          <span class="material-symbols-outlined text-on-background" style="font-variation-settings:'FILL' 1;">pets</span>
        </div>
        <div>
          <h1 class="text-display-md font-sans text-on-background tracking-tighter leading-none">NyamNyam</h1>
          <p class="text-label-lg text-on-surface-variant">Coaching Buddy</p>
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

      <!-- User Profile -->
      <div class="mt-auto neo-brutal-border bg-white rounded-xl p-4 flex items-center gap-3">
        <div class="w-12 h-12 rounded-full border-2 border-on-background overflow-hidden bg-nyam-mint flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-on-background" style="font-variation-settings:'FILL' 1;">person</span>
        </div>
        <div class="min-w-0">
          <p class="text-label-lg leading-none truncate">냠냠마스터</p>
          <p class="text-[10px] text-on-surface-variant">Lv.12 훈련사</p>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="ml-sidebar-width flex-1 min-h-screen" style="padding: 40px; max-width: calc(1440px - 280px);">
      <RouterView />
    </main>

    <!-- Floating Action Button -->
    <button
      class="fixed bottom-10 right-10 w-20 h-20 bg-primary neo-brutal-border rounded-full flex items-center justify-center neo-brutal-shadow hover:scale-110 active:translate-y-2 transition-all z-50 text-white"
      aria-label="빠른 식단 기록"
      @click="$router.push('/meals/search')"
    >
      <span class="material-symbols-outlined text-4xl" style="font-variation-settings:'FILL' 1;">add</span>
    </button>
  </div>
</template>

<script setup>
const navItems = [
  { to: '/',        label: '홈',        icon: 'home' },
  { to: '/log',     label: '식단 기록',  icon: 'restaurant_menu' },
  { to: '/routine', label: '운동 루틴',  icon: 'fitness_center' },
  { to: '/dashboard', label: '대시보드',  icon: 'dashboard' },
  { to: '/my',      label: '마이페이지', icon: 'person' },
]
</script>
