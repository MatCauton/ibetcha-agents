import { apiClient } from './client';
import type {
  FriendsListResponse,
  UserSearchResponse,
  FriendRequestResponse,
  FriendRequestsResponse,
  AcceptFriendResponse,
  InviteResponse,
} from '../types/api';

export async function fetchFriends(params?: {
  search?: string;
  cursor?: string;
  limit?: number;
}): Promise<FriendsListResponse> {
  const response = await apiClient.get<FriendsListResponse>('/friends', {
    params,
  });
  return response.data;
}

export async function searchUsers(query: string): Promise<UserSearchResponse> {
  const response = await apiClient.get<UserSearchResponse>(
    '/friends/search',
    { params: { q: query } },
  );
  return response.data;
}

export async function sendFriendRequest(
  targetUserId: string,
): Promise<FriendRequestResponse> {
  const response = await apiClient.post<FriendRequestResponse>(
    '/friends/request',
    { targetUserId },
  );
  return response.data;
}

export async function fetchFriendRequests(): Promise<FriendRequestsResponse> {
  const response =
    await apiClient.get<FriendRequestsResponse>('/friends/requests');
  return response.data;
}

export async function acceptFriendRequest(
  requestId: string,
): Promise<AcceptFriendResponse> {
  const response = await apiClient.post<AcceptFriendResponse>(
    `/friends/request/${requestId}/accept`,
  );
  return response.data;
}

export async function declineFriendRequest(
  requestId: string,
): Promise<void> {
  await apiClient.post(`/friends/request/${requestId}/decline`);
}

export async function removeFriend(userId: string): Promise<void> {
  await apiClient.delete(`/friends/${userId}`);
}

export async function generateInvite(): Promise<InviteResponse> {
  const response = await apiClient.post<InviteResponse>('/friends/invite');
  return response.data;
}
