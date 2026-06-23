<template>
  <section class="neo-brutal-border rounded-xl bg-surface p-8 neo-brutal-card-hover">
    <h2 class="text-headline-lg text-on-background mb-6">현재 Program</h2>

    <p v-if="error && error.status !== 404" class="text-on-surface-variant">
      Program을 불러올 수 없어요
    </p>

    <div v-else-if="error && error.status === 404"
         class="text-center py-8 text-on-surface-variant">
      <span class="material-symbols-outlined text-4xl mb-2">calendar_today</span>
      <p class="text-body-md">아직 Program이 없어요</p>
    </div>

    <p v-else-if="!program" class="text-on-surface-variant">불러오는 중...</p>

    <dl v-else class="grid grid-cols-2 gap-4">
      <div>
        <dt class="text-label-lg text-on-surface-variant">기간</dt>
        <dd class="text-body-md text-on-background mt-1">
          {{ program.startDate }} ~ {{ program.endDate }}
        </dd>
      </div>
      <div>
        <dt class="text-label-lg text-on-surface-variant">목표 칼로리</dt>
        <dd class="text-body-md text-on-background mt-1">{{ program.dailyKcal }} kcal/일</dd>
      </div>
      <div>
        <dt class="text-label-lg text-on-surface-variant">기간</dt>
        <dd class="text-body-md text-on-background mt-1">{{ program.durationWeeks }}주</dd>
      </div>
      <div>
        <dt class="text-label-lg text-on-surface-variant">상태</dt>
        <dd class="text-body-md text-on-background mt-1">{{ program.status }}</dd>
      </div>
      <div v-if="program.aiComment" class="col-span-2">
        <dt class="text-label-lg text-on-surface-variant">AI 코멘트</dt>
        <dd class="text-body-md text-on-background mt-1 leading-relaxed">{{ program.aiComment }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup>
defineProps({
  program: { type: Object, default: null },
  error: { type: Object, default: null },
});
</script>
