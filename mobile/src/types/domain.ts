export enum BetStatus {
  PENDING_ACCEPTANCE = 'PENDING_ACCEPTANCE',
  ACTIVE = 'ACTIVE',
  PENDING_APPROVAL = 'PENDING_APPROVAL',
  PENDING_JURY_VERDICT = 'PENDING_JURY_VERDICT',
  RESOLVED = 'RESOLVED',
  DISPUTED = 'DISPUTED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

export enum ParticipantStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
}

export enum PendingAction {
  NONE = 'NONE',
  ACCEPT_DECLINE = 'ACCEPT_DECLINE',
  APPROVE_OUTCOME = 'APPROVE_OUTCOME',
  PENDING_JURY_VERDICT = 'PENDING_JURY_VERDICT',
}

export enum FriendshipStatus {
  NONE = 'NONE',
  PENDING_SENT = 'PENDING_SENT',
  PENDING_RECEIVED = 'PENDING_RECEIVED',
  FRIENDS = 'FRIENDS',
}

export enum VoteType {
  APPROVE = 'APPROVE',
  DISPUTE = 'DISPUTE',
}

export enum StreakType {
  WIN = 'WIN',
  LOSS = 'LOSS',
}

export enum EvidenceType {
  PHOTO = 'PHOTO',
  VIDEO = 'VIDEO',
}

export enum ApprovalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  DISPUTED = 'DISPUTED',
}

export interface User {
  id: string;
  email: string;
  username?: string;
  displayName?: string;
  avatarUrl?: string;
  bio?: string;
  profileComplete: boolean;
}

export interface HeadToHead {
  wins: number;
  losses: number;
  totalBets?: number;
}

export interface Participant {
  userId: string;
  displayName: string;
  avatarUrl?: string;
  status: ParticipantStatus;
  acceptedAt?: string;
}

export interface Evidence {
  evidenceId: string;
  type: EvidenceType;
  url: string;
  uploadedBy: string;
  uploadedAt: string;
}

export interface OutcomeVote {
  userId: string;
  vote: VoteType;
}

export interface BetOutcome {
  winnerId: string;
  declaredBy: string;
  declaredAt: string;
  approvalStatus: ApprovalStatus;
  votes: OutcomeVote[];
}

export interface Bet {
  betId: string;
  title?: string;
  description: string;
  stake: string;
  status: BetStatus;
  createdAt: string;
  acceptanceDeadline?: string;
  completionDeadline?: string;
  creator: {
    userId: string;
    displayName: string;
    avatarUrl?: string;
  };
  participants: Participant[];
  jury?: {
    userId: string;
    displayName: string;
    avatarUrl?: string;
  } | null;
  evidence?: Evidence[];
  outcome?: BetOutcome | null;
  winCardUrl?: string | null;
  headToHead?: HeadToHead;
  pendingAction?: PendingAction;
}

export interface Friend {
  userId: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  headToHead: HeadToHead;
  lastInteractionAt?: string;
}

export interface FriendRequest {
  requestId: string;
  fromUser: {
    userId: string;
    username: string;
    displayName: string;
    avatarUrl?: string;
  };
  status: 'PENDING';
  createdAt: string;
}

export interface UserStats {
  totalBets: number;
  wins: number;
  losses: number;
  winRate: number;
  currentStreak: {
    type: StreakType;
    count: number;
  };
  activeBets: number;
  disputedBets: number;
}

export interface UserProfile {
  userId: string;
  username: string;
  displayName: string;
  bio?: string;
  avatarUrl?: string;
  joinedAt?: string;
  stats: UserStats;
  topRivals?: {
    userId: string;
    displayName: string;
    avatarUrl?: string;
    headToHead: HeadToHead;
    totalBets: number;
  }[];
  recentBets?: {
    betId: string;
    description: string;
    outcome: 'WON' | 'LOST';
    opponentName: string;
    resolvedAt: string;
  }[];
}

export interface FriendProfile {
  userId: string;
  username: string;
  displayName: string;
  bio?: string;
  avatarUrl?: string;
  stats: {
    totalBets: number;
    wins: number;
    losses: number;
    winRate: number;
    currentStreak: {
      type: StreakType;
      count: number;
    };
  };
  headToHead: HeadToHead & { totalBets: number };
  activeBets: {
    betId: string;
    description: string;
    participants: string[];
    status: BetStatus;
  }[];
  recentResults: {
    betId: string;
    description: string;
    outcome: 'WON' | 'LOST';
    resolvedAt: string;
  }[];
}
