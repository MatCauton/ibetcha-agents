import { useEffect, useRef } from 'react';
import { Alert, Platform } from 'react-native';
import * as Notifications from 'expo-notifications';
import { useRouter } from 'expo-router';
import { useAuthStore } from '../stores/authStore';
import { registerDeviceToken } from '../api/notifications';

// Handle notifications when the app is in the foreground: show an alert.
// Guard against web — expo-notifications requires VAPID config on web.
if (Platform.OS !== 'web') {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });
}

/**
 * Requests push notification permissions, registers the Expo push token with
 * the backend, and wires up foreground/tap handlers.
 *
 * Call this hook once in the root layout after auth is initialized.
 * No-op on web (push notifications require native device support).
 */
export function usePushNotifications(): void {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const notificationListener = useRef<Notifications.EventSubscription | null>(null);
  const responseListener = useRef<Notifications.EventSubscription | null>(null);

  useEffect(() => {
    if (!isAuthenticated) return;
    if (Platform.OS === 'web') return;

    let cancelled = false;

    async function registerToken(): Promise<void> {
      const { status: existingStatus } = await Notifications.getPermissionsAsync();
      let finalStatus = existingStatus;

      if (existingStatus !== 'granted') {
        const { status } = await Notifications.requestPermissionsAsync();
        finalStatus = status;
      }

      if (finalStatus !== 'granted') {
        // User declined — silently skip token registration
        return;
      }

      const tokenData = await Notifications.getExpoPushTokenAsync();
      if (cancelled) return;

      const platform: 'ANDROID' | 'IOS' = Platform.OS === 'ios' ? 'IOS' : 'ANDROID';

      try {
        await registerDeviceToken(tokenData.data, platform);
      } catch {
        // Token registration is best-effort; do not surface to user
        console.warn('[PushNotifications] Failed to register device token with backend');
      }
    }

    // Foreground notification received handler
    notificationListener.current = Notifications.addNotificationReceivedListener(
      (notification) => {
        const title = notification.request.content.title ?? 'iBetcha';
        const body = notification.request.content.body ?? '';
        Alert.alert(title, body);
      },
    );

    // Notification tapped handler — navigate to the relevant bet
    responseListener.current = Notifications.addNotificationResponseReceivedListener(
      (response) => {
        const data = response.notification.request.content.data as Record<string, unknown> | null;
        const betId = data?.betId;
        if (typeof betId === 'string' && betId) {
          router.push(`/bet/${betId}`);
        }
      },
    );

    registerToken();

    return () => {
      cancelled = true;
      notificationListener.current?.remove();
      responseListener.current?.remove();
    };
  }, [isAuthenticated]);
}
