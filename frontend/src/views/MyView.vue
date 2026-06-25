<template>
  <div class="max-w-4xl mx-auto w-full">

    <header class="mb-10">
      <h1 class="text-display-md text-on-background">마이페이지</h1>
    </header>

    <div class="flex flex-col gap-8">
      <NyamSection :nyam="nyam" :error="nyamError" />
      <ProfileSection :profile="profile" :error="profileError" @updated="onProfileUpdated" />
      <ProgramSection :program="program" :error="programError" />
      <ReportSection />
      <BadgeCollectionSection />

      <!-- 로그아웃 -->
      <div class="flex justify-end pt-2">
        <button
          class="flex items-center gap-2 px-6 py-3 neo-brutal-border rounded-xl text-label-lg font-bold text-danger hover:bg-danger hover:text-white hover:-translate-y-0.5 transition-all duration-150"
          @click="logout"
        >
          <span class="material-symbols-outlined text-base">logout</span>
          로그아웃
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { clearTokens } from '@/services/auth';
import { getMyProfile, getNyamStatus, getCurrentProgram } from '@/api/my';
import { logout as logoutApi } from '@/api/auth';
import NyamSection from '@/components/my/NyamSection.vue';
import ProfileSection from '@/components/my/ProfileSection.vue';
import ProgramSection from '@/components/my/ProgramSection.vue';
import ReportSection from '@/components/my/ReportSection.vue';
import BadgeCollectionSection from '@/components/my/BadgeCollectionSection.vue';

const router = useRouter();

const nyam = ref(null);
const profile = ref(null);
const program = ref(null);

const nyamError = ref(null);
const profileError = ref(null);
const programError = ref(null);

onMounted(async () => {
  // 프로필 먼저 로딩 (memberId가 Program 조회에 필요)
  const profileResult = await Promise.allSettled([getMyProfile(), getNyamStatus()]);

  if (profileResult[0].status === 'fulfilled') {
    profile.value = profileResult[0].value;
  } else {
    profileError.value = profileResult[0].reason;
  }

  if (profileResult[1].status === 'fulfilled') {
    nyam.value = profileResult[1].value;
  } else {
    nyamError.value = profileResult[1].reason;
  }

  const programResult = await Promise.allSettled([getCurrentProgram()]);
  if (programResult[0].status === 'fulfilled') {
    program.value = programResult[0].value;
  } else {
    programError.value = programResult[0].reason;
  }
});

function onProfileUpdated(updated) {
  profile.value = updated;
}

async function logout() {
  try {
    await logoutApi();
  } catch {
    // 서버 오류여도 클라이언트 토큰은 항상 정리한다
  } finally {
    clearTokens();
    router.push('/');
  }
}
</script>
