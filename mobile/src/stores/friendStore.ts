import { create } from 'zustand';
import { Share } from 'react-native';
import { FriendshipStatus } from '../types/domain';
import type { Friend, FriendRequest } from '../types/domain';
import type { UserSearchResult } from '../types/api';
import * as friendsApi from '../api/friends';

interface FriendState {
  friends: Friend[];
  friendRequests: FriendRequest[];
  searchResults: UserSearchResult[];
  isLoading: boolean;
  isRefreshing: boolean;
  isSearching: boolean;
  error: string | null;
  hasMore: boolean;
  cursor: string | undefined;
  inviteCode: string | null;
  inviteLoading: boolean;

  fetchFriends: (refresh?: boolean) => Promise<void>;
  fetchFriendRequests: () => Promise<void>;
  searchUsers: (query: string) => Promise<void>;
  sendFriendRequest: (userId: string) => Promise<void>;
  acceptRequest: (requestId: string) => Promise<void>;
  declineRequest: (requestId: string) => Promise<void>;
  removeFriend: (userId: string) => Promise<void>;
  clearSearch: () => void;
  clearError: () => void;
  shareInviteLink: () => Promise<void>;
}

export const useFriendStore = create<FriendState>((set, get) => ({
  friends: [],
  friendRequests: [],
  searchResults: [],
  isLoading: false,
  isRefreshing: false,
  isSearching: false,
  error: null,
  hasMore: false,
  cursor: undefined,
  inviteCode: null,
  inviteLoading: false,

  fetchFriends: async (refresh = false) => {
    if (refresh) {
      set({ isRefreshing: true });
    } else {
      set({ isLoading: true });
    }
    set({ error: null });

    try {
      const response = await friendsApi.fetchFriends({ limit: 50 });
      set({
        friends: response.friends,
        hasMore: response.hasMore,
        cursor: response.cursor,
        isLoading: false,
        isRefreshing: false,
      });
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load friends';
      set({ error: message, isLoading: false, isRefreshing: false });
    }
  },

  fetchFriendRequests: async () => {
    try {
      const response = await friendsApi.fetchFriendRequests();
      set({ friendRequests: response.requests });
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load friend requests';
      set({ error: message });
    }
  },

  searchUsers: async (query: string) => {
    if (query.length < 2) {
      set({ searchResults: [] });
      return;
    }
    set({ isSearching: true });
    try {
      const response = await friendsApi.searchUsers(query);
      set({ searchResults: response.results, isSearching: false });
    } catch (err: unknown) {
      set({ isSearching: false });
      // Silently fail for search — user can retry by typing
    }
  },

  sendFriendRequest: async (userId: string) => {
    try {
      await friendsApi.sendFriendRequest(userId);
      // Update search results to show pending status
      set((state) => ({
        searchResults: state.searchResults.map((r) =>
          r.userId === userId
            ? { ...r, friendshipStatus: FriendshipStatus.PENDING_SENT }
            : r,
        ),
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to send friend request';
      set({ error: message });
      throw err;
    }
  },

  acceptRequest: async (requestId: string) => {
    try {
      const response = await friendsApi.acceptFriendRequest(requestId);
      set((state) => ({
        friendRequests: state.friendRequests.filter(
          (r) => r.requestId !== requestId,
        ),
        friends: [
          ...state.friends,
          {
            userId: response.friend.userId,
            username: response.friend.username,
            displayName: response.friend.displayName,
            headToHead: { wins: 0, losses: 0 },
          },
        ],
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to accept request';
      set({ error: message });
      throw err;
    }
  },

  declineRequest: async (requestId: string) => {
    try {
      await friendsApi.declineFriendRequest(requestId);
      set((state) => ({
        friendRequests: state.friendRequests.filter(
          (r) => r.requestId !== requestId,
        ),
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to decline request';
      set({ error: message });
      throw err;
    }
  },

  removeFriend: async (userId: string) => {
    try {
      await friendsApi.removeFriend(userId);
      set((state) => ({
        friends: state.friends.filter((f) => f.userId !== userId),
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to remove friend';
      set({ error: message });
      throw err;
    }
  },

  clearSearch: () => set({ searchResults: [] }),
  clearError: () => set({ error: null }),

  shareInviteLink: async () => {
    set({ inviteLoading: true });
    try {
      const invite = await friendsApi.createInviteLink();
      set({ inviteCode: invite.code, inviteLoading: false });
      await Share.share({
        message: `Join me on iBetcha! 🎯 Tap to add me as a friend: ${invite.inviteUrl}`,
      });
    } catch {
      set({ inviteLoading: false });
    }
  },
}));
