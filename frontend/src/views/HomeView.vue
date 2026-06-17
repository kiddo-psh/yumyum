<template>
  <section class="home">
    <div class="home__intro">
      <p class="home__eyebrow">
        Frontend foundation
      </p>
      <h1>YumYum application shell</h1>
      <p>
        Routing, shared layout, design tokens, and API access are ready for the
        next feature slice.
      </p>
    </div>

    <div class="home__grid">
      <article class="home__panel">
        <h2>Routes</h2>
        <p>Vue Router is configured with a layout route and a fallback page.</p>
      </article>
      <article class="home__panel">
        <h2>Design</h2>
        <p>Global tokens define colors, spacing, type, radius, and shadows.</p>
      </article>
      <article class="home__panel">
        <h2>API</h2>
        <p>A small fetch client centralizes base URL, headers, and errors.</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.home {
  display: grid;
  gap: var(--space-8);
}

.home__intro {
  max-width: 720px;
}

.home__eyebrow {
  margin: 0 0 var(--space-3);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
  text-transform: uppercase;
}

.home__intro h1 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: clamp(2rem, 5vw, 4rem);
  line-height: 1.05;
}

.home__intro p:last-child {
  max-width: 600px;
  margin: var(--space-4) 0 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-lg);
}

.home__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}

.home__panel {
  min-height: 160px;
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface-raised);
  box-shadow: var(--shadow-sm);
}

.home__panel h2 {
  margin: 0 0 var(--space-3);
  color: var(--color-text-strong);
  font-size: var(--font-size-xl);
}

.home__panel p {
  margin: 0;
  color: var(--color-text-muted);
}

@media (max-width: 760px) {
  .home__grid {
    grid-template-columns: 1fr;
  }
}
</style>
