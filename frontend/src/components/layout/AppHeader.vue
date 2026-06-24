<template>
  <header class="app-header">
    <RouterLink
      class="app-header__brand"
      to="/"
      aria-label="YumYum home"
    >
      <span class="app-header__mark">Y</span>
      <span>YumYum</span>
    </RouterLink>

    <nav
      class="app-header__nav"
      aria-label="Primary navigation"
    >
      <RouterLink to="/">
        Home
      </RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 0 max(16px, calc((100vw - var(--layout-max-width)) / 2));
  border-bottom: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-surface) 92%, transparent);
  backdrop-filter: blur(16px);
}

.app-header__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text);
  font-weight: 700;
  text-decoration: none;
}

.app-header__mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-contrast);
}

.app-header__nav {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.app-header__nav a {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-decoration: none;
}

.app-header__nav a.router-link-active {
  color: var(--color-primary);
}
</style>
