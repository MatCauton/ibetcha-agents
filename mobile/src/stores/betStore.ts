import { create } from 'zustand';
import type { Bet } from '../types/domain';
import type { CreateBetRequest } from '../types/api';
import * as betsApi from '../api/bets';

interface BetState {
  bets: Bet[];
  currentBet: Bet | null;
  isLoading: boolean;
  isRefreshing: boolean;
  error: string | null;
  hasMore: boolean;
  cursor: string | undefined;

  fetchBets: (refresh?: boolean) => Promise<void>;
  fetchMoreBets: () => Promise<void>;
  fetchBetById: (betId: string) => Promise<void>;
  createBet: (data: CreateBetRequest) => Promise<Bet>;
  acceptBet: (betId: string) => Promise<void>;
  declineBet: (betId: string) => Promise<void>;
  completeBet: (betId: string, winnerId: string) => Promise<void>;
  voteBet: (betId: string, vote: 'APPROVE' | 'DISPUTE') => Promise<void>;
  concedeBet: (betId: string) => Promise<void>;
  submitJuryVerdict: (betId: string, approved: boolean, winnerId?: string) => Promise<void>;
  clearCurrentBet: () => void;
  clearError: () => void;
}

export const useBetStore = create<BetState>((set, get) => ({
  bets: [],
  currentBet: null,
  isLoading: false,
  isRefreshing: false,
  error: null,
  hasMore: false,
  cursor: undefined,

  fetchBets: async (refresh = false) => {
    if (refresh) {
      set({ isRefreshing: true });
    } else {
      set({ isLoading: true });
    }
    set({ error: null });

    try {
      const response = await betsApi.fetchBets({ limit: 20 });
      set({
        bets: response.bets,
        hasMore: response.hasMore,
        cursor: response.cursor,
        isLoading: false,
        isRefreshing: false,
      });
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load bets';
      set({ error: message, isLoading: false, isRefreshing: false });
    }
  },

  fetchMoreBets: async () => {
    const { hasMore, cursor } = get();
    if (!hasMore || !cursor) return;

    try {
      const response = await betsApi.fetchBets({ cursor, limit: 20 });
      set((state) => ({
        bets: [...state.bets, ...response.bets],
        hasMore: response.hasMore,
        cursor: response.cursor,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load more bets';
      set({ error: message });
    }
  },

  fetchBetById: async (betId: string) => {
    set({ isLoading: true, error: null });
    try {
      const bet = await betsApi.fetchBetById(betId);
      set({ currentBet: bet, isLoading: false });
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load bet';
      set({ error: message, isLoading: false });
    }
  },

  createBet: async (data: CreateBetRequest) => {
    set({ isLoading: true, error: null });
    try {
      const bet = await betsApi.createBet(data);
      set((state) => ({
        bets: [bet, ...state.bets],
        isLoading: false,
      }));
      return bet;
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to create bet';
      set({ error: message, isLoading: false });
      throw err;
    }
  },

  acceptBet: async (betId: string) => {
    try {
      const response = await betsApi.acceptBet(betId);
      set((state) => ({
        bets: state.bets.map((b) =>
          b.betId === betId ? { ...b, status: response.status as Bet['status'] } : b,
        ),
        currentBet:
          state.currentBet?.betId === betId
            ? { ...state.currentBet, status: response.status as Bet['status'] }
            : state.currentBet,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to accept bet';
      set({ error: message });
      throw err;
    }
  },

  declineBet: async (betId: string) => {
    try {
      await betsApi.declineBet(betId);
      set((state) => ({
        bets: state.bets.filter((b) => b.betId !== betId),
        currentBet:
          state.currentBet?.betId === betId ? null : state.currentBet,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to decline bet';
      set({ error: message });
      throw err;
    }
  },

  completeBet: async (betId: string, winnerId: string) => {
    try {
      const response = await betsApi.completeBet(betId, { winnerId });
      set((state) => ({
        bets: state.bets.map((b) =>
          b.betId === betId ? { ...b, status: response.status as Bet['status'] } : b,
        ),
        currentBet:
          state.currentBet?.betId === betId
            ? { ...state.currentBet, status: response.status as Bet['status'] }
            : state.currentBet,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to complete bet';
      set({ error: message });
      throw err;
    }
  },

  voteBet: async (betId: string, vote: 'APPROVE' | 'DISPUTE') => {
    try {
      const response = await betsApi.voteBet(betId, { vote });
      set((state) => ({
        bets: state.bets.map((b) =>
          b.betId === betId ? { ...b, status: response.status as Bet['status'] } : b,
        ),
        currentBet:
          state.currentBet?.betId === betId
            ? { ...state.currentBet, status: response.status as Bet['status'] }
            : state.currentBet,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to vote';
      set({ error: message });
      throw err;
    }
  },

  concedeBet: async (betId: string) => {
    try {
      const response = await betsApi.concedeBet(betId);
      set((state) => ({
        bets: state.bets.map((b) =>
          b.betId === betId ? { ...b, status: response.status as Bet['status'] } : b,
        ),
        currentBet:
          state.currentBet?.betId === betId
            ? { ...state.currentBet, status: response.status as Bet['status'] }
            : state.currentBet,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to concede';
      set({ error: message });
      throw err;
    }
  },

  submitJuryVerdict: async (betId: string, approved: boolean, winnerId?: string) => {
    try {
      const response = await betsApi.submitJuryVerdict(betId, approved, winnerId);
      set((state) => ({
        bets: state.bets.map((b) =>
          b.betId === betId ? { ...b, status: response.status as Bet['status'] } : b,
        ),
        currentBet:
          state.currentBet?.betId === betId
            ? { ...state.currentBet, status: response.status as Bet['status'] }
            : state.currentBet,
      }));
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to submit jury verdict';
      set({ error: message });
      throw err;
    }
  },

  clearCurrentBet: () => set({ currentBet: null }),
  clearError: () => set({ error: null }),
}));
