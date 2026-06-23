import { ApiError, apiClient } from '@/services/apiClient';

export async function getMyRoutines() {
  const data = await apiClient.get('/routines');
  // 백엔드 미연결 시 dev 서버가 index.html(문자열)을 반환할 수 있어 배열만 신뢰한다.
  return Array.isArray(data) ? data : [];
}

export async function getSplitOptions(daysPerWeek) {
  const data = await apiClient.get('/routines/split-options', { params: { daysPerWeek } });
  return Array.isArray(data) ? data : [];
}

export async function createAiRoutine({ hasExistingRoutine, daysPerWeek, splitType }) {
  const data = await apiClient.post('/routines/ai', {
    hasExistingRoutine,
    daysPerWeek,
    splitType,
  });

  if (!data || typeof data !== 'object' || !Array.isArray(data.exercises)) {
    throw new ApiError('Unexpected routine response', { status: 0, data: null });
  }

  return data;
}
