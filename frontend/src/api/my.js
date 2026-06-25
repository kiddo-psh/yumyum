import { apiClient } from '@/services/apiClient';

export function getMyProfile() {
  return apiClient.get('/members/me');
}

export function updateMyProfile({ sex, birthYear, heightCm, weightKg, activityLevel, healthGoal, targetDate }) {
  return apiClient.patch('/members/me', { sex, birthYear, heightCm, weightKg, activityLevel, healthGoal, targetDate });
}

export function getNyamStatus() {
  return apiClient.get('/nyam/status');
}

export function getCurrentProgram() {
  return apiClient.get('/programs/current');
}
