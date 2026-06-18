<template>
  <div class="app-shell">
    <aside
      class="app-sidebar"
      aria-label="Primary navigation"
    >
      <RouterLink
        class="app-sidebar__brand"
        to="/"
        aria-label="YumYum Coach home"
      >
        <span
          class="app-sidebar__mark"
          aria-hidden="true"
        >
          <svg viewBox="0 0 24 24">
            <path d="M7 3v8" />
            <path d="M11 3v8" />
            <path d="M7 7h4" />
            <path d="M16 3v18" />
            <path d="M16 9h2.5a2.5 2.5 0 0 0 0-5H16" />
          </svg>
        </span>
        <span class="app-sidebar__title">YumYum Coach</span>
        <span class="app-sidebar__subtitle">AI NUTRITION</span>
      </RouterLink>

      <nav class="app-sidebar__nav">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.to"
          :to="item.to"
          class="app-sidebar__link"
        >
          <span
            class="app-sidebar__icon"
            aria-hidden="true"
          >
            <svg viewBox="0 0 24 24">
              <path
                v-for="path in item.paths"
                :key="path"
                :d="path"
              />
            </svg>
          </span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="app-shell__main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
const navigationItems = [
  {
    to: '/',
    label: 'Home',
    paths: ['M4 11 12 4l8 7', 'M6 10v10h12V10', 'M10 20v-6h4v6'],
  },
  {
    to: '/log',
    label: 'Log',
    paths: ['M4 6h10', 'M4 12h8', 'm14 16 2 2 4-5', 'M4 18h5'],
  },
  {
    to: '/recommend',
    label: 'Recommend',
    paths: ['M12 3v4', 'M12 17v4', 'M3 12h4', 'M17 12h4', 'm6 6 2.5 2.5', 'm15.5 15.5 2.5 2.5', 'm18 6-2.5 2.5', 'm8.5 15.5-2.5 2.5'],
  },
  {
    to: '/report',
    label: 'Report',
    paths: ['M5 19V9', 'M12 19V5', 'M19 19v-7', 'M3 19h18'],
  },
  {
    to: '/my',
    label: 'My',
    paths: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8', 'M5 21a7 7 0 0 1 14 0'],
  },
];
</script>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 268px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--color-surface);
  color: var(--color-text);
}

.app-shell__main {
  width: min(100%, calc(var(--layout-max-width) - 268px));
  padding: 42px 44px 56px;
}

.app-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 34px;
  height: 100vh;
  padding: 28px 26px 28px 22px;
  border-right: 3px solid #d7e5cf;
  border-radius: 0 28px 28px 0;
  background:
    radial-gradient(circle at 48px 80px, rgb(88 204 2 / 12%), transparent 62px),
    linear-gradient(180deg, #fbfff8 0%, #ffffff 56%, #f6fbf2 100%);
  box-shadow: 8px 0 0 #e5efdD;
}

.app-sidebar__brand {
  display: grid;
  justify-items: center;
  gap: var(--space-2);
  color: var(--color-text);
  text-decoration: none;
}

.app-sidebar__mark {
  display: grid;
  width: 76px;
  height: 76px;
  place-items: center;
  border: 3px solid var(--color-primary-contrast);
  border-radius: 22px;
  background: var(--color-primary);
  box-shadow: var(--shadow-press-green);
  color: var(--color-primary-contrast);
}

.app-sidebar__mark svg,
.app-sidebar__icon svg {
  width: 1.2em;
  height: 1.2em;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.5;
}

.app-sidebar__title {
  margin-top: var(--space-2);
  color: var(--color-success);
  font-size: 1.125rem;
  font-weight: 900;
}

.app-sidebar__subtitle {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 800;
  letter-spacing: 0;
}

.app-sidebar__nav {
  display: grid;
  gap: 18px;
}

.app-sidebar__link {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 68px;
  padding: 17px 18px;
  border: 3px solid transparent;
  border-radius: 20px;
  color: var(--color-text);
  font-size: 1.0625rem;
  font-weight: 900;
  text-decoration: none;
  transition:
    transform 150ms ease,
    box-shadow 150ms ease,
    background-color 150ms ease;
}

.app-sidebar__link.router-link-exact-active {
  border-color: #7c6300;
  background: var(--color-accent);
  box-shadow: var(--shadow-press-yellow);
  color: var(--color-accent-strong);
}

.app-sidebar__link:not(.router-link-exact-active):hover {
  background: #f5fbef;
  box-shadow: 0 5px 0 #e1eadb;
  transform: translateY(-1px);
}

.app-sidebar__link:active {
  transform: translateY(5px);
  box-shadow: none;
}

.app-sidebar__icon {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
}

.app-sidebar__icon svg {
  width: 1.35em;
  height: 1.35em;
}

@media (max-width: 960px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-sidebar {
    position: static;
    z-index: 4;
    height: auto;
    padding: 16px;
    border-right: 0;
    border-bottom: 3px solid var(--color-border);
    border-radius: 0;
    box-shadow: none;
  }

  .app-sidebar__brand {
    grid-template-columns: auto 1fr;
    justify-items: start;
  }

  .app-sidebar__mark {
    grid-row: span 2;
    width: 48px;
    height: 48px;
  }

  .app-sidebar__nav {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    overflow-x: auto;
  }

  .app-sidebar__link {
    justify-content: center;
    min-width: 112px;
    min-height: 60px;
  }

  .app-shell__main {
    width: 100%;
    padding: 24px 16px 40px;
  }
}

.app-shell {
  background: #ffffff;
}

.app-shell__main {
  padding: 40px 44px 56px;
}

.app-sidebar {
  gap: 30px;
  border-right: 1px solid #e4ecdf;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
}

.app-sidebar__mark {
  width: 64px;
  height: 64px;
  border: 1px solid #b9d9ad;
  border-radius: 18px;
  background: #f8fbf5;
  box-shadow: none;
  color: var(--color-success);
}

.app-sidebar__title {
  color: var(--color-success);
  font-size: 1.05rem;
  font-weight: 850;
}

.app-sidebar__subtitle {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 700;
  letter-spacing: 0;
}

.app-sidebar__nav {
  gap: 10px;
}

.app-sidebar__link {
  min-height: 56px;
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 16px;
  color: var(--color-text);
  font-size: 1rem;
  font-weight: 800;
}

.app-sidebar__link.router-link-exact-active {
  border-color: #b9d9ad;
  background: #f3faef;
  box-shadow: none;
  color: var(--color-success);
}

.app-sidebar__link:not(.router-link-exact-active):hover {
  border-color: #e4ecdf;
  background: #fbfdf9;
  box-shadow: none;
  transform: none;
}

.app-sidebar__link:active {
  transform: translateY(1px);
}

.app-sidebar__icon {
  width: 24px;
  height: 24px;
}

@media (max-width: 960px) {
  .app-sidebar {
    border-bottom: 1px solid #e4ecdf;
  }
}

@media (max-width: 520px) {
  .app-shell,
  .app-sidebar,
  .app-shell__main {
    max-width: 100vw;
    min-width: 0;
  }

  .app-shell__main {
    padding: 24px 16px 40px;
  }

  .app-sidebar {
    padding: 16px;
  }

  .app-sidebar__brand,
  .app-sidebar__nav {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  .app-sidebar__nav {
    grid-template-columns: repeat(5, minmax(58px, 1fr));
    gap: 6px;
  }

  .app-sidebar__link {
    flex-direction: column;
    gap: 4px;
    min-width: 0;
    min-height: 58px;
    padding: 8px 6px;
    font-size: 0.72rem;
  }

  .app-sidebar__icon {
    width: 20px;
    height: 20px;
  }
}
</style>
