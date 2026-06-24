<template>
  <div class="max-w-2xl mx-auto">
    <div class="flex items-center gap-3 mb-8">
      <RouterLink to="/" class="w-10 h-10 flex items-center justify-center neo-brutal-border rounded-full hover:bg-surface transition-colors">
        <span class="material-symbols-outlined">arrow_back</span>
      </RouterLink>
      <h1 class="text-headline-lg">사진으로 기록</h1>
    </div>

    <!-- idle: 사진 선택 -->
    <div v-if="phase === 'idle'" class="bg-white neo-brutal-border rounded-xl p-8 text-center neo-brutal-card-hover">
      <span class="material-symbols-outlined text-[80px] text-primary mb-4 block" style="font-variation-settings:'FILL' 1;">camera_enhance</span>
      <p class="text-body-lg text-on-surface-variant mb-6">식사 사진을 찍거나 갤러리에서 선택하세요</p>
      <p v-if="sizeError" class="text-danger text-sm font-bold mb-4">{{ sizeError }}</p>
      <label class="cursor-pointer inline-block bg-primary text-white px-8 py-3 neo-brutal-border rounded-lg font-bold hover:-translate-y-1 transition-transform">
        <span class="material-symbols-outlined align-middle mr-2">add_a_photo</span>사진 선택
        <input
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden"
          @change="onFileSelected"
        />
      </label>
    </div>

    <!-- analyzing: 분석 중 -->
    <div v-else-if="phase === 'analyzing'" class="bg-white neo-brutal-border rounded-xl p-12 text-center">
      <span class="material-symbols-outlined text-[60px] text-primary animate-spin block mb-4">progress_activity</span>
      <p class="text-headline-md">AI가 음식을 분석하는 중...</p>
      <p class="text-body-md text-on-surface-variant mt-2">잠시만 기다려주세요</p>
    </div>

    <!-- result: 분석 완료 -->
    <div v-else-if="phase === 'result' || phase === 'saving'">
      <!-- 미리보기 이미지 -->
      <div class="mb-6 neo-brutal-border rounded-xl overflow-hidden">
        <img :src="previewUrl" alt="분석된 사진" class="w-full max-h-64 object-cover" />
      </div>

      <!-- AI 코멘트 -->
      <div class="bg-nyam-mint neo-brutal-border rounded-xl p-5 mb-6 flex items-start gap-3">
        <span class="material-symbols-outlined text-primary flex-shrink-0" style="font-variation-settings:'FILL' 1;">smart_toy</span>
        <p class="text-body-md">{{ analysisResult.aiComment }}</p>
      </div>

      <!-- 감지된 음식 없음 -->
      <div v-if="analysisResult.detectedItems.length === 0" class="bg-white neo-brutal-border rounded-xl p-8 text-center mb-6">
        <p class="text-on-surface-variant">음식을 찾지 못했어요. 다시 촬영해보세요.</p>
        <button class="mt-4 px-6 py-2 neo-brutal-border rounded-lg font-bold hover:-translate-y-1 transition-transform" @click="reset">다시 촬영</button>
      </div>

      <!-- 감지된 음식 목록 -->
      <div v-else>
        <div class="flex items-center justify-between mb-4">
          <h2 class="font-bold text-lg">감지된 음식</h2>
          <span class="text-primary font-bold">총 {{ Math.round(analysisResult.totalKcal) }} kcal</span>
        </div>

        <div class="space-y-3 mb-8">
          <div
            v-for="(item, idx) in analysisResult.detectedItems"
            :key="item.name + '_' + idx"
            class="bg-white neo-brutal-border rounded-xl p-4 flex items-center justify-between"
          >
            <div>
              <p class="font-bold">{{ item.name }}</p>
              <p class="text-sm text-on-surface-variant">약 {{ item.estimatedGrams }}g</p>
            </div>
            <div class="text-right">
              <p class="font-bold text-primary">{{ Math.round(item.kcal) }} kcal</p>
              <p class="text-xs text-on-surface-variant">
                단{{ Math.round(item.proteinG) }}g 탄{{ Math.round(item.carbG) }}g 지{{ Math.round(item.fatG) }}g
              </p>
            </div>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            class="flex-1 py-3 neo-brutal-border rounded-xl font-bold hover:-translate-y-1 transition-transform"
            :disabled="phase === 'saving'"
            @click="reset"
          >
            다시 촬영
          </button>
          <button
            class="flex-1 py-3 bg-primary text-white neo-brutal-border rounded-xl font-bold disabled:opacity-50 hover:-translate-y-1 transition-transform"
            :disabled="phase === 'saving'"
            @click="saveMeal"
          >
            {{ phase === 'saving' ? '저장 중...' : '식단에 추가' }}
          </button>
        </div>
      </div>
    </div>

    <!-- error -->
    <div v-else-if="phase === 'error'" class="bg-white neo-brutal-border rounded-xl p-8 text-center">
      <span class="material-symbols-outlined text-[60px] text-danger block mb-4">error</span>
      <p class="text-headline-md mb-2">분석 실패</p>
      <p class="text-body-md text-on-surface-variant mb-6">{{ errorMessage }}</p>
      <button class="px-8 py-3 neo-brutal-border rounded-xl font-bold hover:-translate-y-1 transition-transform" @click="reset">다시 시도</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { analyzePhoto, recordPhotoMeal } from '@/api/meal'
import { useBadgeStore } from '@/stores/badge'

const router = useRouter()
const badgeStore = useBadgeStore()

const phase = ref('idle')        // 'idle' | 'analyzing' | 'result' | 'saving' | 'error'
const analysisResult = ref(null)
const selectedFile = ref(null)
const previewUrl = ref(null)
const errorMessage = ref('')
const sizeError = ref('')

const MAX_SIZE_BYTES = 10 * 1024 * 1024  // 10MB

function onFileSelected(event) {
  const file = event.target.files[0]
  if (!file) return

  sizeError.value = ''
  if (file.size > MAX_SIZE_BYTES) {
    sizeError.value = '사진 크기가 10MB를 초과합니다. 더 작은 사진을 선택해주세요.'
    event.target.value = ''
    return
  }

  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  startAnalysis()
}

async function startAnalysis() {
  phase.value = 'analyzing'
  try {
    const result = await analyzePhoto(selectedFile.value, inferMealType())
    analysisResult.value = {
      detectedItems: result.detectedItems ?? [],
      totalKcal: result.totalKcal ?? 0,
      aiComment: result.aiComment ?? '',
    }
    phase.value = 'result'
  } catch {
    phase.value = 'error'
    errorMessage.value = '서버 연결에 실패했어요. 잠시 후 다시 시도해주세요.'
  }
}

async function saveMeal() {
  phase.value = 'saving'
  try {
    const meal = await recordPhotoMeal(inferMealType(), analysisResult.value.detectedItems)
    badgeStore.celebrate(meal)
    router.push('/log')
  } catch {
    phase.value = 'error'
    errorMessage.value = '저장에 실패했어요. 다시 시도해주세요.'
  }
}

function reset() {
  phase.value = 'idle'
  analysisResult.value = null
  selectedFile.value = null
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  sizeError.value = ''
  errorMessage.value = ''
}

function inferMealType() {
  const h = new Date().getHours()
  if (h >= 4 && h < 10) return 'BREAKFAST'
  if (h < 15) return 'LUNCH'
  if (h < 20) return 'DINNER'
  return 'SNACK'
}

onUnmounted(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>
