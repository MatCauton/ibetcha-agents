import { apiClient } from './client';
import type {
  LoginRequest,
  RegisterRequest,
  AuthTokenResponse,
  RegisterResponse,
  ProfileSetupRequest,
} from '../types/api';
import type { User } from '../types/domain';

export async function loginWithGoogle(
  idToken: string,
): Promise<AuthTokenResponse> {
  const response = await apiClient.post<AuthTokenResponse>('/auth/oauth/google', {
    idToken,
  });
  return response.data;
}

export async function loginWithApple(
  identityToken: string,
  authorizationCode: string,
): Promise<AuthTokenResponse> {
  const response = await apiClient.post<AuthTokenResponse>('/auth/oauth/apple', {
    identityToken,
    authorizationCode,
  });
  return response.data;
}

export async function login(data: LoginRequest): Promise<AuthTokenResponse> {
  const response = await apiClient.post<AuthTokenResponse>(
    '/auth/login',
    data,
  );
  return response.data;
}

export async function register(
  data: RegisterRequest,
): Promise<AuthTokenResponse> {
  const response = await apiClient.post<AuthTokenResponse>(
    '/auth/register',
    data,
  );
  return response.data;
}

export async function refreshAccessToken(
  refreshToken: string,
): Promise<{ accessToken: string; refreshToken: string; expiresIn: number }> {
  const response = await apiClient.post('/auth/refresh', { refreshToken });
  return response.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken });
}

export async function setupProfile(
  data: ProfileSetupRequest,
): Promise<{ userId: string; username: string; profileComplete: boolean }> {
  const response = await apiClient.post('/profile/setup', data);
  return response.data;
}

export async function getProfile(): Promise<User> {
  const response = await apiClient.get('/profile');
  return response.data;
}
