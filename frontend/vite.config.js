import { fileURLToPath, URL } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const MOCK_DATA = {
  '/api/daily-summary': {
    targetCalories: 1800,
    achievedCalories: 1320,
    totalCarbs: 148,
    totalProtein: 72,
    totalFat: 38,
    currentStreak: 5,
  },
  '/api/calorie-balance': {
    targetCalories: 1800,
    intakeCalories: 1320,
    lastMealRecommendTrigger: false,
  },
  '/api/meals': [
    {
      id: 1,
      items: [
        { foodCode: 'D001', foodName: '현미밥', amountGrams: 200, calories: 320, protein: 6.8 },
        { foodCode: 'D002', foodName: '된장찌개', amountGrams: 300, calories: 145, protein: 11.2 },
        { foodCode: 'D003', foodName: '달걀 프라이', amountGrams: 60, calories: 92, protein: 6.4 },
      ],
    },
    {
      id: 2,
      items: [
        { foodCode: 'D004', foodName: '닭가슴살 구이', amountGrams: 150, calories: 248, protein: 46.5 },
        { foodCode: 'D005', foodName: '샐러드', amountGrams: 200, calories: 88, protein: 2.8 },
        { foodCode: 'D006', foodName: '고구마', amountGrams: 100, calories: 131, protein: 1.5 },
      ],
    },
    {
      id: 3,
      items: [
        { foodCode: 'D007', foodName: '바나나', amountGrams: 120, calories: 107, protein: 1.3 },
        { foodCode: 'D008', foodName: '그릭요거트', amountGrams: 150, calories: 89, protein: 15.0 },
      ],
    },
  ],
  '/api/growth/weekly-calendar': {
    days: [
      { date: '2026-06-16', achievementRate: 1.02 },
      { date: '2026-06-17', achievementRate: 0.78 },
      { date: '2026-06-18', achievementRate: 0.97 },
      { date: '2026-06-19', achievementRate: 1.08 },
      { date: '2026-06-20', achievementRate: 0.82 },
      { date: '2026-06-21', achievementRate: 0 },
      { date: '2026-06-22', achievementRate: 0 },
    ],
  },
  '/api/weights': [
    { date: '2026-06-01', weight: 72.5 },
    { date: '2026-06-04', weight: 72.1 },
    { date: '2026-06-07', weight: 71.8 },
    { date: '2026-06-10', weight: 71.5 },
    { date: '2026-06-13', weight: 71.2 },
    { date: '2026-06-16', weight: 71.0 },
    { date: '2026-06-19', weight: 70.6 },
  ],
};

function devMockPlugin() {
  return {
    name: 'dev-mock',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const path = req.url?.split('?')[0];
        if (!path?.startsWith('/api/')) return next();

        const data = MOCK_DATA[path];
        if (data === undefined) return next();

        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(data));
      });
    },
  };
}

export default defineConfig({
  plugins: [vue(), devMockPlugin()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
});
