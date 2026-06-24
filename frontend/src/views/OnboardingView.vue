<template>
  <div class="onboarding-root">
    <div class="onboarding-card">
      <!-- 상단 헤더 + 진행 바 -->
      <div class="onboarding-header">
        <div class="onboarding-brand">
          <span class="material-symbols-outlined brand-icon" style="font-variation-settings:'FILL' 1;">cruelty_free</span>
          <span class="brand-name">냠냠코치</span>
        </div>
        <div class="step-progress">
          <div
            v-for="i in TOTAL_STEPS"
            :key="i"
            :class="['step-bar', i <= step ? 'step-bar--active' : '']"
          />
        </div>
        <p class="step-label">{{ STEP_TITLES[step - 1] }}</p>
      </div>

      <!-- 단계 콘텐츠 -->
      <div class="onboarding-body">
        <Transition :name="transitionName" mode="out-in">

          <!-- Step 1: 기본 정보 -->
          <div v-if="step === 1" key="step1" class="step-content">
            <h2 class="step-heading">기본 정보를 알려주세요</h2>
            <p class="step-sub">맞춤형 칼로리와 운동 계획을 만들어 드릴게요.</p>

            <!-- 성별 -->
            <div class="field-group">
              <label class="field-label">성별</label>
              <div class="sex-buttons">
                <button
                  v-for="opt in SEX_OPTIONS"
                  :key="opt.value"
                  type="button"
                  @click="form.sex = opt.value"
                  :class="['sex-btn', form.sex === opt.value ? 'sex-btn--active' : '']"
                >
                  <span class="material-symbols-outlined" style="font-variation-settings:'FILL' 1;">{{ opt.icon }}</span>
                  {{ opt.label }}
                </button>
              </div>
            </div>

            <!-- 출생연도 / 키 / 몸무게 -->
            <div class="info-grid">
              <div class="field-group">
                <label class="field-label">출생연도</label>
                <div class="input-wrap">
                  <input
                    v-model.number="form.birthYear"
                    type="number"
                    min="1924"
                    :max="currentYear - 10"
                    placeholder="1995"
                    class="text-input"
                  />
                  <span class="input-unit">년</span>
                </div>
              </div>
              <div class="field-group">
                <label class="field-label">키</label>
                <div class="input-wrap">
                  <input
                    v-model.number="form.heightCm"
                    type="number"
                    step="0.1"
                    min="100"
                    max="250"
                    placeholder="170"
                    class="text-input"
                  />
                  <span class="input-unit">cm</span>
                </div>
              </div>
              <div class="field-group">
                <label class="field-label">몸무게</label>
                <div class="input-wrap">
                  <input
                    v-model.number="form.weightKg"
                    type="number"
                    step="0.1"
                    min="20"
                    max="300"
                    placeholder="65"
                    class="text-input"
                  />
                  <span class="input-unit">kg</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Step 2: 활동 수준 -->
          <div v-else-if="step === 2" key="step2" class="step-content">
            <h2 class="step-heading">평소 활동 수준을 선택해 주세요</h2>
            <p class="step-sub">TDEE(총 에너지 소비량) 계산에 사용돼요.</p>

            <div class="activity-list">
              <button
                v-for="opt in ACTIVITY_OPTIONS"
                :key="opt.value"
                type="button"
                @click="form.activityLevel = opt.value"
                :class="['activity-item', form.activityLevel === opt.value ? 'activity-item--active' : '']"
              >
                <div class="activity-info">
                  <span class="activity-name">{{ opt.label }}</span>
                  <span class="activity-desc">{{ opt.desc }}</span>
                </div>
                <div :class="['activity-radio', form.activityLevel === opt.value ? 'activity-radio--checked' : '']" />
              </button>
            </div>
          </div>

          <!-- Step 3: 건강 목표 -->
          <div v-else key="step3" class="step-content">
            <h2 class="step-heading">건강 목표를 선택해 주세요</h2>
            <p class="step-sub">목표에 따라 칼로리 계획과 AI 코멘트가 달라져요.</p>

            <div class="goal-grid">
              <button
                v-for="opt in GOAL_OPTIONS"
                :key="opt.value"
                type="button"
                @click="form.healthGoal = opt.value"
                :class="['goal-card', form.healthGoal === opt.value ? 'goal-card--active' : '']"
              >
                <span class="material-symbols-outlined goal-icon" style="font-variation-settings:'FILL' 1;">{{ opt.icon }}</span>
                <span class="goal-label">{{ opt.label }}</span>
                <span class="goal-desc">{{ opt.desc }}</span>
              </button>
            </div>

            <!-- 목표 날짜 (선택) -->
            <div class="target-date-wrap">
              <label class="field-label">목표 날짜 <span class="optional">(선택사항)</span></label>
              <input
                v-model="form.targetDate"
                type="date"
                :min="minTargetDate"
                class="text-input date-input"
              />
              <p class="date-hint">목표일을 정해두면 동기부여에 도움이 돼요.</p>
            </div>

            <p v-if="error" class="error-msg">{{ error }}</p>
          </div>

        </Transition>
      </div>

      <!-- 하단 네비게이션 -->
      <div class="onboarding-footer">
        <button
          v-if="step > 1"
          type="button"
          class="btn-prev"
          @click="prev"
        >
          ← 이전
        </button>
        <div v-else />

        <button
          type="button"
          :disabled="!canProceed || submitting"
          :class="['btn-next', step === TOTAL_STEPS ? 'btn-next--submit' : '']"
          @click="next"
        >
          <template v-if="submitting">저장 중...</template>
          <template v-else-if="step === TOTAL_STEPS">시작하기 🎉</template>
          <template v-else>다음 단계 →</template>
        </button>
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
/* ── 레이아웃 ── */
.onboarding-root {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.onboarding-card {
  width: 100%;
  max-width: 680px;
  background: #fff;
  border: 3px solid #1a1a1a;
  border-radius: 20px;
  box-shadow: 6px 6px 0 #1a1a1a;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 헤더 ── */
.onboarding-header {
  background: #1a1a1a;
  padding: 1.75rem 2.5rem 1.5rem;
}

.onboarding-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  color: #fff;
}

.brand-icon {
  font-size: 1.75rem;
}

.brand-name {
  font-size: 1.125rem;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.step-progress {
  display: flex;
  gap: 0.375rem;
  margin-bottom: 0.625rem;
}

.step-bar {
  height: 4px;
  flex: 1;
  border-radius: 2px;
  background: rgba(255,255,255,0.2);
  transition: background 0.3s ease;
}

.step-bar--active {
  background: #a8e6cf;
}

.step-label {
  font-size: 0.8125rem;
  color: rgba(255,255,255,0.55);
  margin: 0;
}

/* ── 바디 ── */
.onboarding-body {
  padding: 2.25rem 2.5rem;
  min-height: 340px;
  flex: 1;
  overflow: hidden;
  position: relative;
}

.step-content {
  width: 100%;
}

.step-heading {
  font-size: 1.375rem;
  font-weight: 900;
  color: #1a1a1a;
  margin: 0 0 0.375rem;
  letter-spacing: -0.02em;
}

.step-sub {
  font-size: 0.875rem;
  color: #777;
  margin: 0 0 1.75rem;
}

/* ── 성별 ── */
.sex-buttons {
  display: flex;
  gap: 0.75rem;
}

.sex-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border: 2px solid #ddd;
  border-radius: 12px;
  background: #f9f9f9;
  font-size: 0.9375rem;
  font-weight: 700;
  color: #555;
  cursor: pointer;
  transition: all 0.15s ease;
}

.sex-btn:hover {
  border-color: #1a1a1a;
  background: #f0f0f0;
}

.sex-btn--active {
  border-color: #1a1a1a;
  background: #1a1a1a;
  color: #fff;
  box-shadow: 3px 3px 0 #a8e6cf;
}

/* ── 정보 그리드 ── */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-top: 1.25rem;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #444;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.optional {
  font-weight: 400;
  color: #999;
  text-transform: none;
  letter-spacing: 0;
}

.input-wrap {
  position: relative;
}

.text-input {
  width: 100%;
  border: 2px solid #ddd;
  border-radius: 10px;
  padding: 0.75rem 2.5rem 0.75rem 0.875rem;
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a1a;
  background: #fff;
  box-sizing: border-box;
  transition: border-color 0.15s;
  outline: none;
}

.text-input:focus {
  border-color: #1a1a1a;
}

.input-unit {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.8125rem;
  font-weight: 600;
  color: #999;
  pointer-events: none;
}

/* ── 활동 수준 ── */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.125rem;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.activity-item:hover {
  border-color: #bbb;
  background: #f3f3f3;
}

.activity-item--active {
  border-color: #1a1a1a;
  background: #fff;
  box-shadow: 3px 3px 0 #a8e6cf;
}

.activity-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.activity-name {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #1a1a1a;
}

.activity-desc {
  font-size: 0.8125rem;
  color: #888;
}

.activity-radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #ccc;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.activity-radio--checked {
  border-color: #1a1a1a;
  background: #1a1a1a;
  box-shadow: inset 0 0 0 4px #fff;
}

/* ── 건강 목표 ── */
.goal-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
  margin-bottom: 1.75rem;
}

.goal-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 1.125rem 0.75rem;
  border: 2px solid #e8e8e8;
  border-radius: 14px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.15s ease;
}

.goal-card:hover {
  border-color: #bbb;
  background: #f3f3f3;
}

.goal-card--active {
  border-color: #1a1a1a;
  background: #1a1a1a;
  box-shadow: 3px 3px 0 #a8e6cf;
}

.goal-icon {
  font-size: 1.75rem;
  color: #888;
  transition: color 0.15s;
}

.goal-card--active .goal-icon {
  color: #a8e6cf;
}

.goal-label {
  font-size: 0.875rem;
  font-weight: 800;
  color: #1a1a1a;
  transition: color 0.15s;
}

.goal-card--active .goal-label {
  color: #fff;
}

.goal-desc {
  font-size: 0.75rem;
  color: #999;
  transition: color 0.15s;
}

.goal-card--active .goal-desc {
  color: rgba(255,255,255,0.6);
}

/* ── 목표 날짜 ── */
.target-date-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-top: 0.25rem;
}

.date-input {
  padding-right: 0.875rem;
  max-width: 240px;
}

.date-hint {
  font-size: 0.8125rem;
  color: #aaa;
  margin: 0;
}

.error-msg {
  margin-top: 1rem;
  color: #e53e3e;
  font-size: 0.875rem;
  font-weight: 600;
}

/* ── 푸터 ── */
.onboarding-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 2.5rem 2rem;
  border-top: 1px solid #f0f0f0;
}

.btn-prev {
  padding: 0.625rem 1.25rem;
  border: 2px solid #ddd;
  border-radius: 10px;
  background: #fff;
  font-size: 0.875rem;
  font-weight: 700;
  color: #666;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-prev:hover {
  border-color: #bbb;
  background: #f5f5f5;
}

.btn-next {
  padding: 0.75rem 1.75rem;
  border: 2px solid #1a1a1a;
  border-radius: 10px;
  background: #1a1a1a;
  font-size: 0.9375rem;
  font-weight: 800;
  color: #fff;
  cursor: pointer;
  box-shadow: 3px 3px 0 #a8e6cf;
  transition: all 0.15s ease;
}

.btn-next:hover:not(:disabled) {
  transform: translate(-1px, -1px);
  box-shadow: 4px 4px 0 #a8e6cf;
}

.btn-next:active:not(:disabled) {
  transform: translate(1px, 1px);
  box-shadow: 2px 2px 0 #a8e6cf;
}

.btn-next:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-next--submit {
  background: #2d7a5e;
  border-color: #2d7a5e;
}

/* ── 슬라이드 트랜지션 ── */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(48px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-48px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-48px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(48px);
}
</style>
