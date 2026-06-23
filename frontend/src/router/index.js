import { createRouter, createWebHistory } from 'vue-router';

import { isAuthenticated } from '@/services/auth';
import MainLayout from '@/layouts/MainLayout.vue';
import DashboardView from '@/views/DashboardView.vue';
import HomeView from '@/views/HomeView.vue';
import LoginView from '@/views/LoginView.vue';
import LogView from '@/views/LogView.vue';
import MealActionView from '@/views/MealActionView.vue';
import MyView from '@/views/MyView.vue';
import MealPhotoView from '@/views/MealPhotoView.vue';
import NotFoundView from '@/views/NotFoundView.vue';
import OAuthCallbackView from '@/views/OAuthCallbackView.vue';
import PlaceholderView from '@/views/PlaceholderView.vue';
import RoutineOnboardingView from '@/views/RoutineOnboardingView.vue';
import RoutineView from '@/views/RoutineView.vue';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      title: '로그인',
      public: true,
    },
  },
  {
    path: '/oauth/callback',
    name: 'oauth-callback',
    component: OAuthCallbackView,
    meta: {
      title: '로그인 처리 중',
      public: true,
    },
  },
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
        component: LogView,
        meta: {
          title: '식단 기록',
          navLabel: 'Log',
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
        component: MealPhotoView,
        meta: {
          title: '사진으로 기록',
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
        path: 'routine',
        name: 'routine',
        component: RoutineView,
        meta: {
          title: '운동 루틴',
          navLabel: 'Routine',
        },
      },
      {
        path: 'routine/onboarding',
        name: 'routine-onboarding',
        component: RoutineOnboardingView,
        meta: {
          title: '운동 루틴 만들기',
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
        component: MyView,
        meta: {
          title: '마이',
          navLabel: 'My',
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
      public: true,
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

// 모든 기능은 로그인 후 사용 가능하다. meta.public 라우트만 비로그인 접근을 허용하고,
// 그 외 라우트는 토큰이 없으면 로그인 화면으로 보낸다.
router.beforeEach((to) => {
  if (to.meta.public) {
    // 이미 로그인된 상태에서 로그인 화면으로 가면 홈으로 되돌린다.
    if (to.name === 'login' && isAuthenticated()) {
      return { name: 'home' };
    }
    return true;
  }

  if (isAuthenticated()) {
    return true;
  }

  return { name: 'login' };
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | YumYum` : 'YumYum';
});

export default router;
