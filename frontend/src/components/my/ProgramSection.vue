<template>
  <section class="neo-brutal-border rounded-xl bg-white p-8">
    <h2 class="text-headline-lg font-bold text-on-background mb-7">현재 Program</h2>

    <p v-if="error && error.status !== 404" class="text-body-md text-on-surface-variant">
      Program을 불러올 수 없어요
    </p>

    <div v-else-if="error && error.status === 404"
         class="text-center py-12 text-on-surface-variant">
      <span class="material-symbols-outlined text-5xl mb-3 block">calendar_today</span>
      <p class="text-body-lg">아직 Program이 없어요</p>
    </div>

    <p v-else-if="!program" class="text-body-md text-on-surface-variant">불러오는 중...</p>

    <template v-else>
      <!-- 기간 + 상태 -->
      <div class="flex items-center justify-between pb-7 mb-7 border-b-[3px] border-on-background">
        <div>
          <p class="text-label-lg text-on-surface-variant">기간</p>
          <p class="text-headline-md font-bold text-on-background mt-1">
            {{ program.startDate }} ~ {{ program.endDate }}
          </p>
          <p class="text-label-lg text-on-surface-variant mt-0.5">{{ program.durationWeeks }}주 프로그램</p>
        </div>
        <div class="px-4 py-2 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold shrink-0">
          {{ program.status }}
        </div>
      </div>

      <!-- 하루 칼로리 -->
      <p class="text-label-lg text-on-surface-variant mb-3">하루 영양 목표</p>
      <div class="flex items-baseline gap-2 mb-7">
        <span class="text-numeral-xl text-on-background leading-none">{{ program.dailyKcal }}</span>
        <span class="text-headline-md text-on-surface-variant">kcal</span>
      </div>

      <!-- 3대 영양소 -->
      <div class="grid grid-cols-3 gap-6 mb-6">
        <div>
          <p class="text-label-lg text-on-surface-variant">단백질</p>
          <div class="flex items-baseline gap-1 mt-2">
            <span class="text-headline-lg font-bold text-protein">{{ Math.round(program.targetProtein) }}</span>
            <span class="text-body-md text-on-surface-variant">g</span>
          </div>
        </div>
        <div>
          <p class="text-label-lg text-on-surface-variant">탄수화물</p>
          <div class="flex items-baseline gap-1 mt-2">
            <span class="text-headline-lg font-bold text-carbs">{{ Math.round(program.targetCarb) }}</span>
            <span class="text-body-md text-on-surface-variant">g</span>
          </div>
        </div>
        <div>
          <p class="text-label-lg text-on-surface-variant">지방</p>
          <div class="flex items-baseline gap-1 mt-2">
            <span class="text-headline-lg font-bold text-fat">{{ Math.round(program.targetFat) }}</span>
            <span class="text-body-md text-on-surface-variant">g</span>
          </div>
        </div>
      </div>

      <!-- 칼로리 기준 비율 바 -->
      <div class="h-7 flex rounded-xl overflow-hidden neo-brutal-border">
        <div class="bg-protein transition-all duration-700" :style="{ width: proteinPct + '%' }" />
        <div class="bg-carbs transition-all duration-700" :style="{ width: carbPct + '%' }" />
        <div class="bg-fat transition-all duration-700" :style="{ width: fatPct + '%' }" />
      </div>
      <div class="flex justify-between mt-3 mb-7">
        <span class="text-label-lg text-on-surface-variant">단백질 {{ proteinPct }}%</span>
        <span class="text-label-lg text-on-surface-variant">탄수 {{ carbPct }}%</span>
        <span class="text-label-lg text-on-surface-variant">지방 {{ fatPct }}%</span>
      </div>

      <!-- AI 코멘트 -->
      <div v-if="program.aiComment" class="bg-surface neo-brutal-border rounded-xl p-7">
        <div class="flex items-center gap-3 mb-5">
          <img src="/nyam/nyam_coach.png" alt="냠냠코치" class="w-9 h-9 object-contain flex-shrink-0" />
          <p class="text-headline-md font-bold text-on-background">AI 플랜 안내</p>
        </div>
        <p class="text-body-lg text-on-background" style="line-height: 1.8;">{{ program.aiComment }}</p>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  program: { type: Object, default: null },
  error: { type: Object, default: null },
})

const totalKcalFromMacros = computed(() => {
  if (!props.program) return 1
  return props.program.targetProtein * 4 + props.program.targetCarb * 4 + props.program.targetFat * 9
})

const proteinPct = computed(() =>
  Math.round((props.program?.targetProtein * 4 / totalKcalFromMacros.value) * 100)
)
const carbPct = computed(() =>
  Math.round((props.program?.targetCarb * 4 / totalKcalFromMacros.value) * 100)
)
const fatPct = computed(() =>
  Math.round((props.program?.targetFat * 9 / totalKcalFromMacros.value) * 100)
)
</script>
