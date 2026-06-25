import { defineStore } from 'pinia';
import { ref } from 'vue';

import { getMyProfile } from '@/api/my';

export const useMemberStore = defineStore('member', () => {
  const profile = ref(null);
  const loading = ref(false);
  const error = ref(null);

  async function loadProfile() {
    if (profile.value) return;
    if (loading.value) return;
    loading.value = true;
    error.value = null;
    try {
      profile.value = await getMyProfile();
    } catch (err) {
      error.value = err;
    } finally {
      loading.value = false;
    }
  }

  return { profile, loading, error, loadProfile };
});
