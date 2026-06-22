import { apiClient } from '@/services/apiClient';

export function getDailySummary(date) {
  return apiClient.get('/daily-summary', { params: { date } });
}

export function getCalorieBalance(date) {
  return apiClient.get('/calorie-balance', { params: { date } });
}

export function listMeals(date) {
  return apiClient.get('/meals', { params: { date } });
}

export function getWeeklyCalendar(weekOffset = 0) {
  return apiClient.get('/growth/weekly-calendar', { params: { weekOffset } });
}

export function getDailyProgress() {
  return apiClient.get('/growth/progress');
}

export function getLastMealRecommendation() {
  return apiClient.post('/meals/last-recommend');
}

export function getWeightHistory() {
  return apiClient.get('/weights');
}

export function sendChatMessage(message) {
  return apiClient.post('/ai/chat', { message });
}

export function searchFoods(query) {
  return apiClient.get('/foods', { params: { query } });
}

export function deleteMeal(mealId) {
  return apiClient.delete(`/meals/${mealId}`);
}

export function addMealItem(mealId, { foodCode, amountGrams }) {
  return apiClient.post(`/meals/${mealId}/items`, { foodCode, amountGrams: Number(amountGrams) });
}

export function recordMeal({ type, date, foodCode, amountGrams }) {
  const body = {
    date,
    items: [{ foodCode, amountGrams: Number(amountGrams) }],
  };
  if (type) body.type = type;
  return apiClient.post('/meals', body);
}
