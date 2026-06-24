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

    <template v-else>
      <!-- 기본 정보 -->
      <dl class="grid grid-cols-2 gap-4 mb-6">
        <div>
          <dt class="text-label-lg text-on-surface-variant">기간</dt>
          <dd class="text-body-md text-on-background mt-1">
            {{ program.startDate }} ~ {{ program.endDate }}
            <span class="text-on-surface-variant text-sm ml-1">({{ program.durationWeeks }}주)</span>
          </dd>
        </div>
        <div>
          <dt class="text-label-lg text-on-surface-variant">상태</dt>
          <dd class="text-body-md text-on-background mt-1">{{ program.status }}</dd>
        </div>
      </dl>

      <!-- 하루 영양 목표 -->
      <div class="neo-brutal-border rounded-xl p-5 bg-background mb-4">
        <p class="text-label-lg text-on-surface-variant mb-3">하루 영양 목표</p>
        <div class="grid grid-cols-4 gap-3 text-center">
          <div>
            <p class="text-display-sm font-black text-on-background">{{ program.dailyKcal }}</p>
            <p class="text-label-sm text-on-surface-variant">kcal</p>
          </div>
          <div>
            <p class="text-display-sm font-black" style="color: #e8734a;">{{ Math.round(program.targetProtein) }}</p>
            <p class="text-label-sm text-on-surface-variant">단백질 g</p>
          </div>
          <div>
            <p class="text-display-sm font-black" style="color: #4a90d9;">{{ Math.round(program.targetCarb) }}</p>
            <p class="text-label-sm text-on-surface-variant">탄수화물 g</p>
          </div>
          <div>
            <p class="text-display-sm font-black" style="color: #f0b429;">{{ Math.round(program.targetFat) }}</p>
            <p class="text-label-sm text-on-surface-variant">지방 g</p>
          </div>
        </div>

        <!-- 칼로리 기준 비율 바 -->
        <div class="mt-4 flex rounded-full overflow-hidden h-2">
          <div :style="{ width: proteinPct + '%', background: '#e8734a' }" />
          <div :style="{ width: carbPct + '%', background: '#4a90d9' }" />
          <div :style="{ width: fatPct + '%', background: '#f0b429' }" />
        </div>
        <div class="flex justify-between text-xs text-on-surface-variant mt-1">
          <span>단백질 {{ proteinPct }}%</span>
          <span>탄수 {{ carbPct }}%</span>
          <span>지방 {{ fatPct }}%</span>
        </div>
      </div>

      <!-- 플랜 안내 코멘트 -->
      <div v-if="program.aiComment" class="bg-nyam-mint/20 neo-brutal-border rounded-xl p-4">
        <p class="text-label-sm text-on-surface-variant mb-1">플랜 안내</p>
        <p class="text-body-md text-on-background leading-relaxed">{{ program.aiComment }}</p>
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
