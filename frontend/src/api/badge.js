import { apiClient } from '@/services/apiClient';

/**
 * 회원의 전체 뱃지 현황을 조회한다.
 * 잠긴 뱃지도 조건과 함께 내려온다(earned:false).
 * 응답: { badges: BadgeItem[] }
 *   BadgeItem = { code, name, icon, description, category, earned, earnedAt }
 */
export function getBadgeCollection() {
  return apiClient.get('/badges');
}
