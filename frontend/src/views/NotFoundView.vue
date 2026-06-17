<template>
  <main class="not-found">
    <p class="not-found__code">
      404
    </p>
    <h1>Page not found</h1>
    <RouterLink to="/">
      Return home
    </RouterLink>
  </main>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  place-content: center;
  justify-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
  background: var(--color-surface);
  color: var(--color-text);
  text-align: center;
}

.not-found__code {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.not-found h1 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: var(--font-size-2xl);
}

.not-found a {
  color: var(--color-primary);
  font-weight: 700;
}
</style>
