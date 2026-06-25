import { computed, ref } from 'vue';
import { getCurrentProgram } from '@/api/my';
import { getWeeklyReports } from '@/api/report';

// 현재 프로그램의 주간 리포트 목록을 불러오는 공통 로직.
// ReportView(목록), MyView 리포트 섹션, 홈 알림 배너가 공유한다.
export function useWeeklyReports() {
  const reports = ref([]);
  const loading = ref(true);
  const error = ref(false);
  const noProgram = ref(false);

  // 가장 최근(가장 큰 id) 리포트 — 홈 배너의 "새 리포트" 판단에 사용
  const latest = computed(() =>
    reports.value.length
      ? reports.value.reduce((acc, r) => (r.id > acc.id ? r : acc))
      : null
  );

  async function load() {
    loading.value = true;
    error.value = false;
    noProgram.value = false;
    try {
      const program = await getCurrentProgram();
      reports.value = await getWeeklyReports(program.programId);
    } catch (e) {
      if (e?.status === 404) {
        noProgram.value = true;
      } else {
        error.value = true;
      }
    } finally {
      loading.value = false;
    }
  }

  return { reports, latest, loading, error, noProgram, load };
}
