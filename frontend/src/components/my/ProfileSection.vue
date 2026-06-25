<template>
  <section class="neo-brutal-border rounded-xl bg-white p-8">
    <div class="flex items-center justify-between mb-7">
      <h2 class="text-headline-lg font-bold text-on-background">내 프로필</h2>
      <button v-if="!editing && profile"
              class="flex items-center gap-1.5 px-4 py-2 neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-all duration-150"
              @click="startEdit">
        <span class="material-symbols-outlined text-base">edit</span>
        수정
      </button>
    </div>

    <!-- 에러 -->
    <p v-if="error" class="text-body-md text-on-surface-variant">프로필을 불러올 수 없어요</p>

    <!-- 로딩 -->
    <p v-else-if="!profile" class="text-body-md text-on-surface-variant">불러오는 중...</p>

    <!-- 조회 모드 -->
    <div v-else-if="!editing">

      <!-- 신체 스탯 (키 / 몸무게) -->
      <div class="grid grid-cols-2 gap-px mb-6">
        <div v-for="stat in physicalStats" :key="stat.label" class="bg-white px-6 py-7 text-center">
          <p class="text-label-lg text-on-surface-variant mb-2">{{ stat.label }}</p>
          <div class="flex items-baseline justify-center gap-1">
            <span class="text-numeral-xl text-on-background leading-none">{{ stat.value }}</span>
            <span class="text-headline-md text-on-surface-variant">{{ stat.unit }}</span>
          </div>
        </div>
      </div>

      <!-- 기타 정보 (아이콘 + 레이블 + 값) -->
      <dl class="grid grid-cols-2 gap-x-8 gap-y-5">
        <div v-for="item in secondaryItems" :key="item.label" class="flex items-center gap-3">
          <span
            class="material-symbols-outlined text-2xl text-on-surface-variant shrink-0"
            style="font-variation-settings:'FILL' 1;"
          >{{ item.icon }}</span>
          <div>
            <dt class="text-label-lg text-on-surface-variant">{{ item.label }}</dt>
            <dd class="text-body-lg font-bold text-on-background">{{ item.value }}</dd>
          </div>
        </div>
      </dl>

    </div>

    <!-- 수정 모드 -->
    <form v-else class="space-y-5" @submit.prevent="save">
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">성별</label>
          <select v-model="form.sex"
                  class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary">
            <option value="MALE">남성</option>
            <option value="FEMALE">여성</option>
          </select>
        </div>
        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">생년</label>
          <input v-model.number="form.birthYear" type="number" min="1900" max="2020"
                 class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        </div>
        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">키 (cm)</label>
          <input v-model.number="form.heightCm" type="number" step="0.1"
                 class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        </div>
        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">몸무게 (kg)</label>
          <input v-model.number="form.weightKg" type="number" step="0.1"
                 class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary" />
        </div>
        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">활동량</label>
          <select v-model="form.activityLevel"
                  class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary">
            <option value="SEDENTARY">거의 안 함</option>
            <option value="LIGHTLY_ACTIVE">가벼운 활동</option>
            <option value="MODERATELY_ACTIVE">보통 활동</option>
            <option value="VERY_ACTIVE">활발한 활동</option>
            <option value="EXTRA_ACTIVE">매우 활발</option>
          </select>
        </div>
        <div>
          <label class="text-label-lg font-bold text-on-background mb-2 block">건강 목표</label>
          <select v-model="form.healthGoal"
                  class="w-full neo-brutal-border rounded-xl px-4 py-3 text-body-md bg-background focus:outline-none focus:ring-2 focus:ring-primary">
            <option value="DIET">다이어트</option>
            <option value="MUSCLE">근육 증가</option>
            <option value="HEALTH">건강 유지</option>
            <option value="DISEASE">질환 관리</option>
          </select>
        </div>
      </div>

      <p v-if="saveError" class="text-danger text-label-lg font-bold p-4 bg-surface neo-brutal-border rounded-xl">
        저장에 실패했어요. 다시 시도해주세요.
      </p>

      <div class="flex gap-3">
        <button type="submit"
                class="flex-1 py-4 bg-primary text-on-primary neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-all duration-150 disabled:opacity-40"
                :disabled="saving">
          {{ saving ? '저장 중...' : '저장' }}
        </button>
        <button type="button"
                class="px-8 py-4 neo-brutal-border rounded-xl text-label-lg font-bold hover:-translate-y-0.5 transition-all duration-150"
                @click="cancelEdit">취소</button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue';
import { updateMyProfile } from '@/api/my';

const props = defineProps({
  profile: { type: Object, default: null },
  error: { type: Object, default: null },
});
const emit = defineEmits(['updated']);

const editing = ref(false);
const saving = ref(false);
const saveError = ref(null);
const form = ref({});

const SEX_LABELS = { MALE: '남성', FEMALE: '여성' };
const ACTIVITY_LABELS = {
  SEDENTARY: '거의 안 함', LIGHTLY_ACTIVE: '가벼운 활동',
  MODERATELY_ACTIVE: '보통 활동', VERY_ACTIVE: '활발한 활동', EXTRA_ACTIVE: '매우 활발',
};
const GOAL_LABELS = { DIET: '다이어트', MUSCLE: '근육 증가', HEALTH: '건강 유지', DISEASE: '질환 관리' };

const physicalStats = computed(() => {
  if (!props.profile) return [];
  const p = props.profile;
  return [
    { label: '키', value: p.heightCm, unit: 'cm' },
    { label: '몸무게', value: p.weightKg, unit: 'kg' },
  ];
});

const secondaryItems = computed(() => {
  if (!props.profile) return [];
  const p = props.profile;
  return [
    { label: '성별',    value: SEX_LABELS[p.sex] ?? p.sex,                          icon: 'person' },
    { label: '생년',    value: `${p.birthYear}년`,                                   icon: 'cake' },
    { label: '활동량',  value: ACTIVITY_LABELS[p.activityLevel] ?? p.activityLevel,  icon: 'sprint' },
    { label: '건강 목표', value: GOAL_LABELS[p.healthGoal] ?? p.healthGoal,          icon: 'flag' },
  ];
});

function startEdit() {
  form.value = { ...props.profile };
  saveError.value = null;
  editing.value = true;
}

function cancelEdit() {
  editing.value = false;
}

async function save() {
  saving.value = true;
  saveError.value = null;
  try {
    const updated = await updateMyProfile(form.value);
    emit('updated', updated);
    editing.value = false;
  } catch {
    saveError.value = true;
  } finally {
    saving.value = false;
  }
}
</script>
