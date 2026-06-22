import { API_BASE_URL } from '@/config/env';

const JSON_CONTENT_TYPE = 'application/json';

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

function getStoredAccessToken() {
  return localStorage.getItem('accessToken') ?? sessionStorage.getItem('accessToken');
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
  const headers = new Headers(options.headers);
  const token = getStoredAccessToken();

  if (options.body && !headers.has('content-type')) {
    headers.set('content-type', JSON_CONTENT_TYPE);
  }

  if (token && !headers.has('authorization')) {
    headers.set('authorization', `Bearer ${token}`);
  }

  const { params, ...fetchOptions } = options;
  const url = appendQueryParams(resolveUrl(path), params);

  const response = await fetch(url, {
    ...fetchOptions,
    headers,
  });
  const data = await parseResponse(response);

  if (!response.ok) {
    throw new ApiError('API request failed', {
      status: response.status,
      data,
    });
  }

  return data;
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
