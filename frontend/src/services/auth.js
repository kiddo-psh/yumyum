import { API_BASE_URL } from '@/config/env';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

function trimTrailingSlash(value) {
  return value.endsWith('/') ? value.slice(0, -1) : value;
}

// 카카오 인가 요청은 백엔드 시큐리티 필터가 처리한다(context-path `/api` 포함).
// 백엔드 → 카카오 → 백엔드 성공 핸들러 → frontend.redirect-url(`/oauth/callback`)로 토큰과 함께 리다이렉트된다.
export const KAKAO_LOGIN_URL = `${trimTrailingSlash(API_BASE_URL)}/oauth2/authorization/kakao`;

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

export function isAuthenticated() {
  return Boolean(getAccessToken());
}

export function storeTokens({ accessToken, refreshToken }) {
  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
}

// 카카오 인가 페이지로 전체 페이지 전환(SPA 라우팅 아님).
export function startKakaoLogin() {
  window.location.assign(KAKAO_LOGIN_URL);
}
