import { API_BASE_URL } from '@/config/env';
import { clearTokens, getAccessToken, getRefreshToken, storeTokens } from '@/services/auth';

const JSON_CONTENT_TYPE = 'application/json';
const LOGIN_PATH = '/login';
const REISSUE_PATH = '/auth/reissue';

export class ApiError extends Error {
  constructor(message, { status, data } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

function resolveUrl(path) {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  const base = API_BASE_URL.endsWith('/') ? API_BASE_URL : `${API_BASE_URL}/`;
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path;

  // URL constructor requires an absolute base; resolve relative base against current location
  const absoluteBase = new URL(base, window.location.href).href;
  return new URL(normalizedPath, absoluteBase).href;
}

function appendQueryParams(url, params) {
  if (!params) {
    return url;
  }

  const nextUrl = new URL(url);

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }

    nextUrl.searchParams.set(key, value);
  });

  return nextUrl.toString();
}

function redirectToLogin() {
  clearTokens();
  if (window.location.pathname !== LOGIN_PATH) {
    window.location.assign(LOGIN_PATH);
  }
}

// 동시에 여러 요청이 401을 받아도 재발급은 한 번만 수행한다.
// (백엔드가 Refresh Token을 회전시키므로, 중복 호출 시 두 번째부터는 이미 삭제된 토큰으로 실패한다.)
let reissueInFlight = null;

function reissueAccessToken() {
  if (reissueInFlight) {
    return reissueInFlight;
  }

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return Promise.resolve(null);
  }

  reissueInFlight = (async () => {
    try {
      const response = await fetch(resolveUrl(REISSUE_PATH), {
        method: 'POST',
        headers: { 'content-type': JSON_CONTENT_TYPE },
        body: JSON.stringify({ refreshToken }),
      });

      if (!response.ok) {
        return null;
      }

      const data = await response.json();
      storeTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
      return data.accessToken ?? null;
    } catch {
      return null;
    } finally {
      reissueInFlight = null;
    }
  })();

  return reissueInFlight;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') ?? '';

  if (response.status === 204) {
    return null;
  }

  if (contentType.includes(JSON_CONTENT_TYPE)) {
    return response.json();
  }

  return response.text();
}

export async function request(path, options = {}) {
  return performRequest(path, options, true);
}

async function performRequest(path, options, allowReissue) {
  const headers = new Headers(options.headers);
  const token = getAccessToken();

  if (options.body && !headers.has('content-type')) {
    headers.set('content-type', JSON_CONTENT_TYPE);
  }

  if (token) {
    headers.set('authorization', `Bearer ${token}`);
  }

  const { params, ...fetchOptions } = options;
  const url = appendQueryParams(resolveUrl(path), params);

  const response = await fetch(url, {
    ...fetchOptions,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 && allowReissue && path !== REISSUE_PATH) {
      // Access Token이 거부되면 Refresh Token으로 재발급 후 1회 재시도한다.
      const newToken = await reissueAccessToken();
      if (newToken) {
        return performRequest(path, options, false);
      }
    }

    // 재발급까지 실패한 401이면 세션을 정리하고 로그인 화면으로 보낸다.
    if (response.status === 401) {
      console.warn(`[auth] 401 (재발급 실패) — ${fetchOptions.method ?? 'GET'} ${path}`);
      redirectToLogin();
    }

    // parseResponse 실패(비JSON 바디 등)는 null 로 폴백한다.
    let errData = null;
    try { errData = await parseResponse(response); } catch { /* ignore */ }
    throw new ApiError('API request failed', {
      status: response.status,
      data: errData,
    });
  }

  return parseResponse(response);
}

export const apiClient = {
  get(path, options) {
    return request(path, { ...options, method: 'GET' });
  },
  post(path, body, options) {
    return request(path, {
      ...options,
      method: 'POST',
      body: JSON.stringify(body),
    });
  },
  put(path, body, options) {
    return request(path, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },
  patch(path, body, options) {
    return request(path, {
      ...options,
      method: 'PATCH',
      body: JSON.stringify(body),
    });
  },
  delete(path, options) {
    return request(path, { ...options, method: 'DELETE' });
  },
};
