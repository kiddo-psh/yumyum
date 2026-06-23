<template>
  <section class="neo-brutal-border rounded-xl bg-surface p-8">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-headline-lg text-on-background">내 프로필</h2>
      <button v-if="!editing && profile"
              class="text-label-lg text-primary underline"
              @click="startEdit">수정</button>
    </div>

    <!-- 에러 -->
    <p v-if="error" class="text-on-surface-variant">프로필을 불러올 수 없어요</p>

    <!-- 로딩 -->
    <p v-else-if="!profile" class="text-on-surface-variant">불러오는 중...</p>

    <!-- 조회 모드 -->
    <dl v-else-if="!editing" class="grid grid-cols-2 gap-4">
      <div v-for="item in profileItems" :key="item.label">
        <dt class="text-label-lg text-on-surface-variant">{{ item.label }}</dt>
        <dd class="text-body-md text-on-background mt-1">{{ item.value }}</dd>
      </div>
    </dl>

    <!-- 수정 모드 -->
    <form v-else class="grid grid-cols-2 gap-4" @submit.prevent="save">
      <div>
        <label class="text-label-lg text-on-surface-variant">성별</label>
        <select v-model="form.sex" class="mt-1 w-full neo-brutal-border rounded-lg px-3 py-2">
          <option value="MALE">남성</option>
          <option value="FEMALE">여성</option>
        </select>
      </div>
      <div>
        <label class="text-label-lg text-on-surface-variant">생년</label>
        <input v-model.number="form.birthYear" type="number" min="1900" max="2020"
               class="mt-1 w-full neo-brutal-border rounded-lg px-3 py-2" />
      </div>
      <div>
        <label class="text-label-lg text-on-surface-variant">키 (cm)</label>
        <input v-model.number="form.heightCm" type="number" step="0.1"
               class="mt-1 w-full neo-brutal-border rounded-lg px-3 py-2" />
      </div>
      <div>
        <label class="text-label-lg text-on-surface-variant">몸무게 (kg)</label>
        <input v-model.number="form.weightKg" type="number" step="0.1"
               class="mt-1 w-full neo-brutal-border rounded-lg px-3 py-2" />
      </div>
      <div>
        <label class="text-label-lg text-on-surface-variant">활동량</label>
        <select v-model="form.activityLevel" class="mt-1 w-full neo-brutal-border rounded-lg px-3 py-2">
          <option value="SEDENTARY">거의 안 함</option>
          <option value="LIGHTLY_ACTIVE">가벼운 활동</option>
          <option value="MODERATELY_ACTIVE">보통 활동</option>
          <option value="VERY_ACTIVE">활발한 활동</option>
          <option value="EXTRA_ACTIVE">매우 활발</option>
        </select>
      </div>
      <div>
        <label class="text-label-lg text-on-surface-variant">건강 목표</label>
        <select v-model="form.healthGoal" class="mt-1 w-full neo-brutal-border rounded-lg px-3 py-2">
          <option value="DIET">다이어트</option>
          <option value="MUSCLE">근육 증가</option>
          <option value="HEALTH">건강 유지</option>
          <option value="DISEASE">질환 관리</option>
        </select>
      </div>

      <div class="col-span-2 flex gap-3 mt-2">
        <button type="submit"
                class="px-6 py-2 bg-primary text-on-primary neo-brutal-border rounded-lg text-label-lg"
                :disabled="saving">
          {{ saving ? '저장 중...' : '저장' }}
        </button>
        <button type="button"
                class="px-6 py-2 neo-brutal-border rounded-lg text-label-lg"
                @click="cancelEdit">취소</button>
      </div>
      <p v-if="saveError" class="col-span-2 text-danger text-label-lg">저장에 실패했어요. 다시 시도해주세요.</p>
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

const profileItems = computed(() => {
  if (!props.profile) return [];
  const p = props.profile;
  return [
    { label: '성별', value: SEX_LABELS[p.sex] ?? p.sex },
    { label: '생년', value: `${p.birthYear}년` },
    { label: '키', value: `${p.heightCm} cm` },
    { label: '몸무게', value: `${p.weightKg} kg` },
    { label: '활동량', value: ACTIVITY_LABELS[p.activityLevel] ?? p.activityLevel },
    { label: '건강 목표', value: GOAL_LABELS[p.healthGoal] ?? p.healthGoal },
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
