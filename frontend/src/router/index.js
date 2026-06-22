import { createRouter, createWebHistory } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import DashboardView from '@/views/DashboardView.vue';
import HomeView from '@/views/HomeView.vue';
import MealActionView from '@/views/MealActionView.vue';
import NotFoundView from '@/views/NotFoundView.vue';
import PlaceholderView from '@/views/PlaceholderView.vue';

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: HomeView,
        meta: {
          title: '홈',
          navLabel: 'Home',
        },
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: {
          title: '대시보드',
          navLabel: 'Dashboard',
        },
      },
      {
        path: 'log',
        name: 'log',
        component: PlaceholderView,
        meta: {
          title: '식단 기록',
          navLabel: 'Log',
          heading: '식단 기록',
          description: '오늘 등록한 Meal 목록과 추가 액션을 확인합니다.',
        },
      },
      {
        path: 'meals/search',
        name: 'meal-search',
        component: MealActionView,
        meta: {
          title: '음식 검색',
          mode: 'search',
        },
      },
      {
        path: 'meals/photo',
        name: 'meal-photo',
        component: PlaceholderView,
        meta: {
          title: '사진으로 기록',
          heading: '사진으로 기록',
          description: '사진 분석 API가 연결되면 이 화면에서 업로드 후 Meal을 생성합니다.',
        },
      },
      {
        path: 'meals/manual',
        name: 'meal-manual',
        component: MealActionView,
        meta: {
          title: '직접 입력',
          mode: 'manual',
        },
      },
      {
        path: 'recommend',
        name: 'recommend',
        component: PlaceholderView,
        meta: {
          title: '추천',
          navLabel: 'Recommend',
          heading: '추천',
          description: '마지막 끼니 추천과 AI 코칭 결과를 모아 볼 수 있는 화면입니다.',
        },
      },
      {
        path: 'report',
        name: 'report',
        component: PlaceholderView,
        meta: {
          title: '리포트',
          navLabel: 'Report',
          heading: '리포트',
          description: '주간 달성률과 WeeklyReport 흐름으로 이동하는 화면입니다.',
        },
      },
      {
        path: 'my',
        name: 'my',
        component: PlaceholderView,
        meta: {
          title: '마이',
          navLabel: 'My',
          heading: '마이',
          description: 'Member, Program, Nyam 상태를 관리하는 개인 화면입니다.',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: {
      title: 'Page not found',
    },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  const accessToken = to.query.accessToken;
  const refreshToken = to.query.refreshToken;

  if (typeof accessToken === 'string') {
    localStorage.setItem('accessToken', accessToken);
  }

  if (typeof refreshToken === 'string') {
    localStorage.setItem('refreshToken', refreshToken);
  }

  if (accessToken || refreshToken) {
    const query = { ...to.query };
    delete query.accessToken;
    delete query.refreshToken;

    return {
      path: to.path,
      query,
      hash: to.hash,
      replace: true,
    };
  }

  return true;
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | YumYum` : 'YumYum';
});

export default router;
