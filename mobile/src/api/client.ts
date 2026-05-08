import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { Platform } from 'react-native';
import { storageGet, storageSet, storageDelete } from '../utils/storage';

const BASE_URL = Platform.select({
  android: 'http://10.0.2.2:8080/api/v1',
  ios: 'http://localhost:8080/api/v1',
  default: 'http://localhost:8080/api/v1',
});

// Registered by authStore to clear auth state when token refresh fails.
// Avoids a circular dependency (authStore → client → authStore).
let onAuthExpired: (() => void) | null = null;
export function setAuthExpiredCallback(cb: () => void): void {
  onAuthExpired = cb;
}

const TOKEN_KEY = 'ibetcha_access_token';
const REFRESH_TOKEN_KEY = 'ibetcha_refresh_token';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue: {
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}[] = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
}

// Request interceptor: attach access token
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = await storageGet(TOKEN_KEY);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Response interceptor: handle 401 with token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    // Skip refresh for auth endpoints themselves
    if (
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh')
    ) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({
          resolve: (token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            resolve(apiClient(originalRequest));
          },
          reject,
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const refreshToken = await storageGet(REFRESH_TOKEN_KEY);
      if (!refreshToken) {
        throw new Error('No refresh token');
      }

      const response = await axios.post(`${BASE_URL}/auth/refresh`, {
        refreshToken,
      });

      const { accessToken, refreshToken: newRefreshToken } = response.data;

      await storageSet(TOKEN_KEY, accessToken);
      await storageSet(REFRESH_TOKEN_KEY, newRefreshToken);

      processQueue(null, accessToken);

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
      }

      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);

      await storageDelete(TOKEN_KEY);
      await storageDelete(REFRESH_TOKEN_KEY);

      // Notify auth store so it clears isAuthenticated and triggers redirect to login
      onAuthExpired?.();

      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

// Token management helpers
export async function setTokens(
  accessToken: string,
  refreshToken: string,
): Promise<void> {
  await storageSet(TOKEN_KEY, accessToken);
  await storageSet(REFRESH_TOKEN_KEY, refreshToken);
}

export async function clearTokens(): Promise<void> {
  await storageDelete(TOKEN_KEY);
  await storageDelete(REFRESH_TOKEN_KEY);
}

export async function getAccessToken(): Promise<string | null> {
  return storageGet(TOKEN_KEY);
}

export async function getRefreshToken(): Promise<string | null> {
  return storageGet(REFRESH_TOKEN_KEY);
}
