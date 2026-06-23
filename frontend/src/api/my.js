import { apiClient } from '@/services/apiClient';

export function getMyProfile() {
  return apiClient.get('/members/me');
}

export function updateMyProfile({ sex, birthYear, heightCm, weightKg, activityLevel, healthGoal }) {
  return apiClient.patch('/members/me', { sex, birthYear, heightCm, weightKg, activityLevel, healthGoal });
}

export function getNyamStatus() {
  return apiClient.get('/nyam/status');
}

export function getCurrentProgram(memberId) {
  return apiClient.get('/programs/current', { params: { memberId } });
}
