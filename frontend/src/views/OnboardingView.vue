<template>
  <div class="min-h-screen bg-sub-background flex items-center justify-center p-5">
    <div class="w-full max-w-[640px] flex flex-col items-center gap-6">

      <!-- 냠냠코치 큰 이미지 -->
      <div class="flex flex-col items-center gap-3">
        <img src="/nyam/nyam_coach.png" alt="냠냠코치" class="w-36 h-36 object-contain" />
        <div class="text-center">
          <p class="text-headline-lg font-bold text-on-background">냠냠코치</p>
          <p class="text-body-md text-on-surface-variant">맞춤 건강 플랜을 만들어드릴게요!</p>
        </div>
      </div>

    <div class="w-full bg-background neo-brutal-border rounded neo-brutal-shadow overflow-hidden">

      <!-- Progress header -->
      <div class="px-8 pt-7 pb-6 border-b-[3px] border-on-background">
        <div class="flex items-center gap-2.5 mb-5">
          <div class="w-8 h-8 bg-nyam-mint rounded-xl neo-brutal-border flex items-center justify-center overflow-hidden flex-shrink-0">
            <img src="/nyam/nyam_logo.png" alt="" aria-hidden="true" class="w-6 h-6 object-contain" />
          </div>
          <span class="text-label-lg text-on-background">냠냠코치</span>
        </div>
        <div class="flex gap-1.5 mb-3" role="progressbar" :aria-valuenow="step" aria-valuemin="1" :aria-valuemax="TOTAL_STEPS" :aria-label="`${step}단계 / ${TOTAL_STEPS}단계 완료`">
          <div
            v-for="i in TOTAL_STEPS"
            :key="i"
            class="h-1.5 flex-1 rounded-full transition-colors duration-300"
            :class="i <= step ? 'bg-primary' : 'bg-outline'"
          />
        </div>
        <p class="text-label-lg text-on-surface-variant">{{ step }} / {{ TOTAL_STEPS }} — {{ STEP_TITLES[step - 1] }}</p>
      </div>

      <!-- Step content -->
      <div class="px-8 py-8 min-h-[380px]">
        <Transition :name="transitionName" mode="out-in">

          <!-- Step 1: 기본 정보 -->
          <div v-if="step === 1" key="step1">
            <h2 class="text-headline-lg text-on-background mb-1">기본 정보를 알려주세요</h2>
            <p class="text-body-md text-on-surface-variant mb-8">맞춤형 칼로리와 운동 계획을 만들어 드릴게요.</p>

            <p class="text-label-lg text-on-background mb-3">성별</p>
            <div class="flex gap-3 mb-7">
              <button
                v-for="opt in SEX_OPTIONS"
                :key="opt.value"
                type="button"
                class="flex-1 flex items-center justify-center gap-2 py-4 rounded neo-brutal-border text-label-lg transition-all duration-150"
                :class="form.sex === opt.value
                  ? 'bg-primary text-on-primary -translate-y-1'
                  : 'bg-background text-on-background hover:bg-surface hover:-translate-y-0.5'"
                :aria-pressed="form.sex === opt.value"
                @click="form.sex = opt.value"
              >
                <span class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">{{ opt.icon }}</span>
                {{ opt.label }}
              </button>
            </div>

            <div class="grid grid-cols-3 gap-4">
              <div v-for="f in bodyFields" :key="f.key">
                <label :for="'field-' + f.key" class="text-label-lg text-on-background mb-2 block">{{ f.label }}</label>
                <div class="relative">
                  <input
                    :id="'field-' + f.key"
                    v-model.number="form[f.key]"
                    type="number"
                    :min="f.min"
                    :max="f.max"
                    :step="f.step"
                    :placeholder="f.placeholder"
                    class="w-full border-2 border-outline rounded-[10px] py-3 pl-4 pr-10 text-body-md text-on-background bg-background focus:border-on-background focus:outline-none transition-colors"
                  />
                  <span class="absolute right-3.5 top-1/2 -translate-y-1/2 text-label-lg text-on-surface-variant pointer-events-none">{{ f.unit }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Step 2: 활동 수준 -->
          <div v-else-if="step === 2" key="step2">
            <h2 class="text-headline-lg text-on-background mb-1">평소 활동 수준을 선택해 주세요</h2>
            <p class="text-body-md text-on-surface-variant mb-8">TDEE(총 에너지 소비량) 계산에 사용돼요.</p>

            <div class="flex flex-col gap-2.5">
              <button
                v-for="opt in ACTIVITY_OPTIONS"
                :key="opt.value"
                type="button"
                class="flex items-center justify-between px-5 py-4 rounded neo-brutal-border text-left transition-all duration-150"
                :class="form.activityLevel === opt.value
                  ? 'bg-primary -translate-y-1'
                  : 'bg-background hover:bg-surface hover:-translate-y-0.5'"
                :aria-pressed="form.activityLevel === opt.value"
                @click="form.activityLevel = opt.value"
              >
                <div>
                  <p class="text-label-lg" :class="form.activityLevel === opt.value ? 'text-on-primary' : 'text-on-background'">{{ opt.label }}</p>
                  <p class="text-body-md mt-0.5" :class="form.activityLevel === opt.value ? 'text-on-primary/80' : 'text-on-surface-variant'">{{ opt.desc }}</p>
                </div>
                <span
                  class="material-symbols-outlined flex-shrink-0 ml-4"
                  :class="form.activityLevel === opt.value ? 'text-on-primary' : 'text-outline'"
                  style="font-variation-settings:'FILL' 1;"
                >{{ form.activityLevel === opt.value ? 'check_circle' : 'radio_button_unchecked' }}</span>
              </button>
            </div>
          </div>

          <!-- Step 3: 건강 목표 -->
          <div v-else key="step3">
            <h2 class="text-headline-lg text-on-background mb-1">건강 목표를 선택해 주세요</h2>
            <p class="text-body-md text-on-surface-variant mb-8">목표에 따라 칼로리 계획과 AI 코멘트가 달라져요.</p>

            <div class="grid grid-cols-2 gap-3 mb-6">
              <button
                v-for="opt in GOAL_OPTIONS"
                :key="opt.value"
                type="button"
                class="flex flex-col items-center gap-2 py-7 rounded neo-brutal-border transition-all duration-150"
                :class="form.healthGoal === opt.value
                  ? 'bg-primary -translate-y-1'
                  : 'bg-background hover:bg-surface hover:-translate-y-0.5'"
                :aria-pressed="form.healthGoal === opt.value"
                @click="form.healthGoal = opt.value"
              >
                <span
                  class="material-symbols-outlined text-3xl"
                  :class="form.healthGoal === opt.value ? 'text-on-primary' : 'text-on-background'"
                  style="font-variation-settings:'FILL' 1;"
                >{{ opt.icon }}</span>
                <p class="text-label-lg" :class="form.healthGoal === opt.value ? 'text-on-primary' : 'text-on-background'">{{ opt.label }}</p>
                <p class="text-xs" :class="form.healthGoal === opt.value ? 'text-on-primary/70' : 'text-on-surface-variant'">{{ opt.desc }}</p>
              </button>
            </div>

            <div>
              <label for="target-date" class="text-label-lg text-on-background mb-2 block">
                목표 날짜
                <span class="text-on-surface-variant font-normal ml-1">(선택사항)</span>
              </label>
              <input
                id="target-date"
                v-model="form.targetDate"
                type="date"
                :min="minTargetDate"
                class="border-2 border-outline rounded-[10px] py-3 px-4 text-body-md text-on-background bg-background focus:border-on-background focus:outline-none transition-colors"
              />
              <p class="text-body-md text-on-surface-variant mt-2">목표일을 정해두면 동기부여에 도움이 돼요.</p>
            </div>

            <p v-if="error" class="text-label-lg text-danger mt-4">{{ error }}</p>
          </div>

        </Transition>
      </div>

      <!-- Footer nav -->
      <div class="px-8 pt-6 pb-8 flex items-center justify-between border-t-[3px] border-on-background">
        <button
          v-if="step > 1"
          type="button"
          class="flex items-center gap-1.5 px-5 py-3 neo-brutal-border rounded neo-brutal-shadow bg-background text-label-lg text-on-background hover:-translate-y-0.5 active:translate-y-1 transition-all duration-150"
          @click="prev"
        >
          <span class="material-symbols-outlined text-base">arrow_back</span>
          이전
        </button>
        <div v-else />

        <button
          type="button"
          :disabled="!canProceed || submitting"
          class="flex items-center gap-2 px-6 py-3 bg-primary text-on-primary neo-brutal-border rounded text-label-lg neo-brutal-shadow transition-all duration-150 disabled:opacity-40 disabled:cursor-not-allowed disabled:shadow-none disabled:translate-y-0"
          :class="canProceed && !submitting ? 'hover:-translate-y-1 hover:shadow-[8px_8px_0_0_#2D2D2D] active:translate-y-1 active:shadow-none' : ''"
          @click="next"
        >
          <span v-if="submitting" class="material-symbols-outlined text-base animate-spin">progress_activity</span>
          <template v-if="submitting">저장 중...</template>
          <template v-else-if="step === TOTAL_STEPS">
            시작하기
            <span class="material-symbols-outlined text-base">rocket_launch</span>
          </template>
          <template v-else>
            다음 단계
            <span class="material-symbols-outlined text-base">arrow_forward</span>
          </template>
        </button>
      </div>

    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { updateMyProfile } from '@/api/my'
import { markOnboardingComplete } from '@/services/auth'

const router = useRouter()

const TOTAL_STEPS = 3
const STEP_TITLES = ['기본 정보', '활동 수준', '건강 목표']
const currentYear = new Date().getFullYear()

const today = new Date()
today.setDate(today.getDate() + 7)
const minTargetDate = today.toISOString().split('T')[0]

const step = ref(1)
const direction = ref('forward')
const submitting = ref(false)
const error = ref('')

const form = reactive({
  sex: '',
  birthYear: null,
  heightCm: null,
  weightKg: null,
  activityLevel: '',
  healthGoal: '',
  targetDate: '',
})

const transitionName = computed(() => direction.value === 'forward' ? 'slide-left' : 'slide-right')

const canProceed = computed(() => {
  if (step.value === 1) return form.sex && form.birthYear && form.heightCm && form.weightKg
  if (step.value === 2) return !!form.activityLevel
  return !!form.healthGoal
})

const SEX_OPTIONS = [
  { value: 'MALE', label: '남성', icon: 'man' },
  { value: 'FEMALE', label: '여성', icon: 'woman' },
]

const ACTIVITY_OPTIONS = [
  { value: 'SEDENTARY', label: '거의 없음', desc: '사무직 위주, 운동 거의 없음' },
  { value: 'LIGHTLY_ACTIVE', label: '가벼운 활동', desc: '주 1-3회 가벼운 운동' },
  { value: 'MODERATELY_ACTIVE', label: '보통 활동', desc: '주 3-5회 중강도 운동' },
  { value: 'VERY_ACTIVE', label: '활발한 활동', desc: '주 6-7회 고강도 운동' },
  { value: 'EXTRA_ACTIVE', label: '매우 활발', desc: '고강도 운동 또는 육체노동' },
]

const GOAL_OPTIONS = [
  { value: 'DIET', label: '다이어트', desc: '체중 감량', icon: 'monitor_weight' },
  { value: 'MUSCLE', label: '근육 증가', desc: '근력·근량 향상', icon: 'fitness_center' },
  { value: 'HEALTH', label: '건강 유지', desc: '균형 잡힌 생활', icon: 'favorite' },
  { value: 'DISEASE', label: '질환 관리', desc: '맞춤 식단 관리', icon: 'medical_services' },
]

const bodyFields = [
  { key: 'birthYear', label: '출생연도', min: 1924, max: currentYear - 10, step: 1, placeholder: '1995', unit: '년' },
  { key: 'heightCm',  label: '키',       min: 100,  max: 250,              step: 0.1, placeholder: '170', unit: 'cm' },
  { key: 'weightKg',  label: '몸무게',   min: 20,   max: 300,              step: 0.1, placeholder: '65',  unit: 'kg' },
]

function prev() {
  direction.value = 'back'
  step.value--
  error.value = ''
}

async function next() {
  if (!canProceed.value) return
  if (step.value < TOTAL_STEPS) {
    direction.value = 'forward'
    step.value++
    return
  }
  await submit()
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    await updateMyProfile({
      sex: form.sex,
      birthYear: form.birthYear,
      heightCm: form.heightCm,
      weightKg: form.weightKg,
      activityLevel: form.activityLevel,
      healthGoal: form.healthGoal,
      targetDate: form.targetDate || undefined,
    })
    markOnboardingComplete()
    router.replace({ name: 'home' })
  } catch {
    error.value = '저장에 실패했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}
.slide-left-enter-from { opacity: 0; transform: translateX(40px); }
.slide-left-leave-to  { opacity: 0; transform: translateX(-40px); }
.slide-right-enter-from { opacity: 0; transform: translateX(-40px); }
.slide-right-leave-to   { opacity: 0; transform: translateX(40px); }

@media (prefers-reduced-motion: reduce) {
  .slide-left-enter-active,
  .slide-left-leave-active,
  .slide-right-enter-active,
  .slide-right-leave-active {
    transition: opacity 0.15s ease;
  }
  .slide-left-enter-from,
  .slide-left-leave-to,
  .slide-right-enter-from,
  .slide-right-leave-to {
    transform: none;
  }
}
</style>
