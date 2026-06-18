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

export function getWeeklyCalendar() {
  return apiClient.get('/growth/weekly-calendar');
}

export function getDailyProgress() {
  return apiClient.get('/growth/progress');
}

export function getLastMealRecommendation() {
  return apiClient.post('/meals/last-recommend');
}

export function searchFoods(query) {
  return apiClient.get('/foods', { params: { query } });
}

export function recordMeal({ type, date, foodId, amountGrams }) {
  return apiClient.post('/meals', {
    type,
    date,
    items: [
      {
        foodId: Number(foodId),
        amountGrams: Number(amountGrams),
      },
    ],
  });
}
