import type {
  Bet,
  Friend,
  FriendRequest,
  FriendProfile,
  User,
  UserProfile,
  FriendshipStatus,
} from './domain';

// ── Auth ──

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  acceptedTerms: boolean;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface RegisterResponse {
  message: string;
  userId: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// ── Friends ──

export interface FriendsListResponse {
  friends: Friend[];
  cursor?: string;
  hasMore: boolean;
}

export interface UserSearchResult {
  userId: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  friendshipStatus: FriendshipStatus;
}

export interface UserSearchResponse {
  results: UserSearchResult[];
}

export interface FriendRequestResponse {
  requestId: string;
  status: 'PENDING';
}

export interface FriendRequestsResponse {
  requests: FriendRequest[];
}

export interface AcceptFriendResponse {
  friendshipId: string;
  friend: {
    userId: string;
    username: string;
    displayName: string;
  };
}

export interface InviteResponse {
  inviteCode: string;
  inviteUrl: string;
  shareMessage: string;
}

// ── Bets ──

export interface CreateBetRequest {
  description: string;
  stake: string;
  participantIds: string[];
  title?: string | null;
  deadline?: string | null;
  juryUserId?: string | null;
  evidenceRequired?: boolean;
}

export interface BetListResponse {
  bets: Bet[];
  cursor?: string;
  hasMore: boolean;
}

export interface AcceptBetResponse {
  betId: string;
  status: string;
  participantStatus: string;
}

export interface CompleteBetRequest {
  winnerId: string;
}

export interface CompleteBetResponse {
  betId: string;
  status: string;
  outcome: {
    winnerId: string;
    declaredBy: string;
    approvalStatus: string;
  };
}

export interface VoteBetRequest {
  vote: 'APPROVE' | 'DISPUTE';
}

export interface VoteBetResponse {
  betId: string;
  status: string;
  votes: { userId: string; vote: string }[];
  majorityReached: boolean;
}

// ── Profile ──

export interface ProfileSetupRequest {
  username: string;
  displayName?: string;
  bio?: string;
}

export interface ProfileUpdateRequest {
  displayName?: string;
  bio?: string;
}

// ── Evidence ──

export interface UploadUrlRequest {
  betId: string;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
}

export interface UploadUrlResponse {
  uploadUrl: string;
  s3Key: string;
  expiresAt: string;
}

export interface ConfirmUploadRequest {
  betId: string;
  s3Key: string;
}

export interface ConfirmUploadResponse {
  evidenceId: string;
  url: string;
  type: string;
}

// ── Shared ──

export interface ApiError {
  message: string;
  code?: string;
  status: number;
  errors?: Record<string, string>;
}

export type {
  Bet,
  Friend,
  FriendRequest,
  FriendProfile,
  User,
  UserProfile,
};
