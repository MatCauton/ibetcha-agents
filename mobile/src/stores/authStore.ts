import { create } from 'zustand';
import { storageGet, storageSet, storageDelete } from '../utils/storage';
import type { User } from '../types/domain';
import * as authApi from '../api/auth';
import { setTokens, clearTokens, getAccessToken, getRefreshToken } from '../api/client';
import type { LoginRequest, RegisterRequest } from '../types/api';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isInitialized: boolean;
  error: string | null;

  initialize: () => Promise<void>;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  loginWithGoogle: (idToken: string) => Promise<void>;
  loginWithApple: (identityToken: string, authorizationCode: string) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: User) => void;
  clearError: () => void;
}

const USER_STORAGE_KEY = 'ibetcha_user';

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  isInitialized: false,
  error: null,

  initialize: async () => {
    try {
      const token = await getAccessToken();
      const userJson = await storageGet(USER_STORAGE_KEY);

      if (token && userJson) {
        const user = JSON.parse(userJson) as User;
        set({ user, isAuthenticated: true, isInitialized: true });
      } else {
        set({ isInitialized: true });
      }
    } catch {
      // On any error during init, treat as not authenticated
      await clearTokens();
      await storageDelete(USER_STORAGE_KEY);
      set({ isInitialized: true, isAuthenticated: false, user: null });
    }
  },

  login: async (data: LoginRequest) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.login(data);
      await setTokens(response.accessToken, response.refreshToken);
      await storageSet(USER_STORAGE_KEY, JSON.stringify(response.user));
      set({
        user: response.user,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Incorrect email or password';
      set({ isLoading: false, error: message });
      throw err;
    }
  },

  register: async (data: RegisterRequest) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.register(data);
      await setTokens(response.accessToken, response.refreshToken);
      await storageSet(USER_STORAGE_KEY, JSON.stringify(response.user));
      set({ user: response.user, isAuthenticated: true, isLoading: false });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Could not create account';
      set({ isLoading: false, error: message });
      throw err;
    }
  },

  loginWithGoogle: async (idToken: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.loginWithGoogle(idToken);
      await setTokens(response.accessToken, response.refreshToken);
      await storageSet(USER_STORAGE_KEY, JSON.stringify(response.user));
      set({ user: response.user, isAuthenticated: true, isLoading: false });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Google sign-in failed';
      set({ isLoading: false, error: message });
      throw err;
    }
  },

  loginWithApple: async (identityToken: string, authorizationCode: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.loginWithApple(identityToken, authorizationCode);
      await setTokens(response.accessToken, response.refreshToken);
      await storageSet(USER_STORAGE_KEY, JSON.stringify(response.user));
      set({ user: response.user, isAuthenticated: true, isLoading: false });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Apple sign-in failed';
      set({ isLoading: false, error: message });
      throw err;
    }
  },

  logout: async () => {
    try {
      const refreshToken = await getRefreshToken();
      if (refreshToken) {
        await authApi.logout(refreshToken).catch(() => {
          // Ignore logout API errors — we still clear local state
        });
      }
    } finally {
      await clearTokens();
      await storageDelete(USER_STORAGE_KEY);
      set({
        user: null,
        isAuthenticated: false,
        error: null,
      });
    }
  },

  setUser: (user: User) => {
    storageSet(USER_STORAGE_KEY, JSON.stringify(user));
    set({ user });
  },

  clearError: () => set({ error: null }),
}));
