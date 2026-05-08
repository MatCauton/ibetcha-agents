import { apiClient } from './client';
import type {
  CreateBetRequest,
  BetListResponse,
  AcceptBetResponse,
  CompleteBetRequest,
  CompleteBetResponse,
  VoteBetRequest,
  VoteBetResponse,
} from '../types/api';
import type { Bet, Evidence, WinCard } from '../types/domain';

export async function fetchBets(params?: {
  status?: string;
  cursor?: string;
  limit?: number;
}): Promise<BetListResponse> {
  const response = await apiClient.get<BetListResponse>('/bets', { params });
  return response.data;
}

export async function fetchBetById(betId: string): Promise<Bet> {
  const response = await apiClient.get<Bet>(`/bets/${betId}`);
  return response.data;
}

export async function createBet(data: CreateBetRequest): Promise<Bet> {
  const response = await apiClient.post<Bet>('/bets', data);
  return response.data;
}

export async function acceptBet(betId: string): Promise<AcceptBetResponse> {
  const response = await apiClient.post<AcceptBetResponse>(
    `/bets/${betId}/accept`,
  );
  return response.data;
}

export async function declineBet(betId: string): Promise<void> {
  await apiClient.post(`/bets/${betId}/decline`);
}

export async function cancelBet(betId: string): Promise<void> {
  await apiClient.post(`/bets/${betId}/cancel`);
}

export async function completeBet(
  betId: string,
  data: CompleteBetRequest,
): Promise<CompleteBetResponse> {
  const response = await apiClient.post<CompleteBetResponse>(
    `/bets/${betId}/complete`,
    data,
  );
  return response.data;
}

export async function voteBet(
  betId: string,
  data: VoteBetRequest,
): Promise<VoteBetResponse> {
  const response = await apiClient.post<VoteBetResponse>(
    `/bets/${betId}/vote`,
    data,
  );
  return response.data;
}

export async function concedeBet(
  betId: string,
): Promise<{ betId: string; status: string; winnerId: string }> {
  const response = await apiClient.post(`/bets/${betId}/concede`);
  return response.data;
}

export interface JuryVerdictRequest {
  approved: boolean;
  winnerId: string | null;
}

export interface JuryVerdictResponse {
  betId: string;
  status: string;
  approved: boolean;
  winnerId: string | null;
}

export async function submitJuryVerdict(
  betId: string,
  approved: boolean,
  winnerId?: string,
): Promise<JuryVerdictResponse> {
  const response = await apiClient.post<JuryVerdictResponse>(
    `/bets/${betId}/jury/verdict`,
    { approved, winnerId: winnerId ?? null },
  );
  return response.data;
}

export interface EvidenceUploadUrlResponse {
  uploadUrl: string;
  s3Key: string;
  contentType: string;
  expiresIn: number;
}

export async function getEvidenceUploadUrl(
  betId: string,
  contentType: string,
): Promise<EvidenceUploadUrlResponse> {
  const response = await apiClient.get<EvidenceUploadUrlResponse>(
    `/bets/${betId}/evidence/upload-url`,
    { params: { contentType } },
  );
  return response.data;
}

export async function registerEvidence(
  betId: string,
  data: { s3Key: string; contentType: string; fileName: string },
): Promise<Evidence> {
  const response = await apiClient.post<Evidence>(
    `/bets/${betId}/evidence`,
    data,
  );
  return response.data;
}

export async function getEvidence(betId: string): Promise<Evidence[]> {
  const response = await apiClient.get<Evidence[]>(`/bets/${betId}/evidence`);
  return response.data;
}

export async function getWinCard(betId: string): Promise<WinCard> {
  const response = await apiClient.get<WinCard>(`/bets/${betId}/win-card`);
  return response.data;
}
