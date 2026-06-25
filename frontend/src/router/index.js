import { createRouter, createWebHistory } from 'vue-router';

import { isAuthenticated, isOnboardingCompleted, markOnboardingComplete, markOnboardingIncomplete } from '@/services/auth';
import { getMyProfile } from '@/api/my';
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
import OnboardingView from '@/views/OnboardingView.vue';
import PlaceholderView from '@/views/PlaceholderView.vue';
import ReportView from '@/views/ReportView.vue';
import RoutineDetailView from '@/views/RoutineDetailView.vue';
import RoutineHistoryView from '@/views/RoutineHistoryView.vue';
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
    path: '/onboarding',
    name: 'onboarding',
    component: OnboardingView,
    meta: {
      title: '시작하기',
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
        path: 'routine/history',
        name: 'routine-history',
        component: RoutineHistoryView,
        meta: {
          title: '운동 기록',
        },
      },
      {
        path: 'routine/:routineId',
        name: 'routine-detail',
        component: RoutineDetailView,
        meta: {
          title: '루틴 상세',
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
        component: ReportView,
        meta: {
          title: '리포트',
          navLabel: 'Report',
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

router.beforeEach(async (to) => {
  if (to.meta.public) {
    if (to.name === 'login' && isAuthenticated()) {
      return { name: 'home' };
    }
    return true;
  }

  if (!isAuthenticated()) {
    return { name: 'login' };
  }

  // /onboarding 진입: 이미 완료한 회원이면 홈으로 돌린다.
  if (to.name === 'onboarding') {
    const status = isOnboardingCompleted();
    if (status === 'true') return { name: 'home' };
    return true;
  }

  // 일반 보호 라우트: localStorage 값 기준 → 없으면 API 확인
  const status = isOnboardingCompleted();
  if (status === 'true') return true;
  if (status === 'false') return { name: 'onboarding' };

  // localStorage에 값이 없는 경우(기존 로그인 유지 등) — API로 확인
  try {
    const profile = await getMyProfile();
    if (profile.onboardingCompleted) {
      markOnboardingComplete();
      return true;
    } else {
      markOnboardingIncomplete();
      return { name: 'onboarding' };
    }
  } catch {
    // API 실패 시 통과시킨다 (로그인된 기존 회원 보호)
    return true;
  }
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | YumYum` : 'YumYum';
});

export default router;
