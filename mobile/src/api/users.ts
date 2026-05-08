import { apiClient } from './client';
import type { UserProfile, FriendProfile } from '../types/domain';
import type { ProfileUpdateRequest } from '../types/api';

export async function fetchOwnProfile(): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>('/profile');
  return response.data;
}

export async function updateProfile(
  data: ProfileUpdateRequest,
): Promise<UserProfile> {
  const response = await apiClient.put<UserProfile>('/profile', data);
  return response.data;
}

export async function fetchUserProfile(
  userId: string,
): Promise<FriendProfile> {
  const response = await apiClient.get<FriendProfile>(
    `/users/${userId}/profile`,
  );
  return response.data;
}
