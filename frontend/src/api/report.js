import { apiClient } from '@/services/apiClient';

export function getWeeklyReports(programId) {
  return apiClient.get(`/programs/${programId}/weekly-reports`);
}

export function getWeeklyReport(programId, weekNumber) {
  return apiClient.get(`/programs/${programId}/weekly-reports/${weekNumber}`);
}
