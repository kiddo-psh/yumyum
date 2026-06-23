import { apiClient } from '@/services/apiClient';

// 서버 측 RefreshToken 무효화. 토큰이 만료됐어도 실패는 무시하고 로컬 정리는 호출 측에서 보장한다.
export function logout() {
  return apiClient.post('/auth/logout');
}
