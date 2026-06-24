import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import { getBadgeCollection } from '@/api/badge';

/**
 * 뱃지 전역 상태.
 *
 * 두 가지 역할을 한다.
 *  1) 컬렉션 캐시 — 마이페이지 도감·사이드바가 공유하는 전체 뱃지 현황(GET /badges).
 *  2) 획득 연출 큐 — 식사/운동 기록 응답에 piggyback 된 streak·newlyEarnedBadges를
 *     순차 노출하기 위한 큐. (BADGE_FRONTEND_HANDOFF.md §3)
 */
export const useBadgeStore = defineStore('badge', () => {
  // --- 컬렉션 ---
  const badges = ref([]);
  const loaded = ref(false);
  const loading = ref(false);
  const error = ref(null);

  const earnedBadges = computed(() => badges.value.filter((b) => b.earned));

  async function loadCollection(force = false) {
    if (loading.value) return;
    if (loaded.value && !force) return;

    loading.value = true;
    error.value = null;
    try {
      const data = await getBadgeCollection();
      badges.value = Array.isArray(data?.badges) ? data.badges : [];
      loaded.value = true;
    } catch (err) {
      error.value = err;
    } finally {
      loading.value = false;
    }
  }

  // --- 획득 연출 큐 ---
  // 항목: { kind: 'streak', current } | { kind: 'badge', badge }
  const queue = ref([]);
  const current = ref(null);

  /**
   * 식사/운동 기록 응답을 받아 연출 큐를 채운다.
   * streak.increased → 스트릭 갱신, 이어서 newlyEarnedBadges 각각을 순차 노출.
   * 새 뱃지를 받으면 컬렉션 캐시를 무효화해 다음 조회 시 갱신되게 한다.
   */
  function celebrate(response) {
    if (!response) return;

    if (response.streak?.increased) {
      queue.value.push({ kind: 'streak', current: response.streak.current });
    }

    const newBadges = response.newlyEarnedBadges ?? [];
    for (const badge of newBadges) {
      queue.value.push({ kind: 'badge', badge });
    }

    if (newBadges.length > 0) {
      loaded.value = false;
    }

    advance();
  }

  /** 다음 연출 항목으로 진행. 큐가 비면 종료. */
  function advance() {
    if (current.value) return;
    current.value = queue.value.shift() ?? null;
  }

  /** 현재 항목을 닫고 다음으로 진행. */
  function dismiss() {
    current.value = null;
    advance();
  }

  return {
    badges,
    loaded,
    loading,
    error,
    earnedBadges,
    loadCollection,
    queue,
    current,
    celebrate,
    dismiss,
  };
});
