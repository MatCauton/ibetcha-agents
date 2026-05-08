import { apiClient } from './client';

export async function registerDeviceToken(
  token: string,
  platform: 'ANDROID' | 'IOS',
): Promise<void> {
  await apiClient.post('/notifications/device-token', { token, platform });
}
