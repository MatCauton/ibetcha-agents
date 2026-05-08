import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useAuthStore } from '../../src/stores/authStore';
import * as friendsApi from '../../src/api/friends';
import type { InviteInfo } from '../../src/api/friends';
import { colors } from '../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';

// Module-level variable to carry the pending invite code through registration.
// Not persistent — if the user leaves and comes back via the link again it resets.
let pendingInviteCode: string | null = null;

export function getPendingInviteCode(): string | null {
  return pendingInviteCode;
}

export function clearPendingInviteCode(): void {
  pendingInviteCode = null;
}

export default function AcceptInviteScreen() {
  const { code } = useLocalSearchParams<{ code: string }>();
  const { user } = useAuthStore();
  const router = useRouter();

  const [inviteInfo, setInviteInfo] = useState<InviteInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const [alreadyFriends, setAlreadyFriends] = useState(false);

  useEffect(() => {
    if (!code) return;

    let cancelled = false;

    async function loadInvite() {
      try {
        const info = await friendsApi.getInviteInfo(code);
        if (!cancelled) {
          setInviteInfo(info);
          setLoading(false);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const status =
            (err as { response?: { status?: number } })?.response?.status;
          if (status === 404) {
            setNotFound(true);
          }
          setLoading(false);
        }
      }
    }

    loadInvite();

    return () => {
      cancelled = true;
    };
  }, [code]);

  async function handleAccept() {
    if (!code) return;
    setAccepting(true);
    try {
      const result = await friendsApi.acceptInvite(code);
      if (result.alreadyFriends) {
        setAlreadyFriends(true);
      } else {
        Alert.alert(t('inviteSuccess'));
        router.replace('/(tabs)/friends');
      }
    } catch {
      Alert.alert(t('error'));
    } finally {
      setAccepting(false);
    }
  }

  function handleSignUp() {
    pendingInviteCode = code;
    router.push('/(auth)/register');
  }

  const avatarLetter =
    inviteInfo?.inviterDisplayName?.charAt(0).toUpperCase() ?? '?';

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (notFound) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorIcon}>!</Text>
        <Text style={styles.errorTitle}>{t('inviteInvalid')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* App name */}
      <Text style={styles.appName}>{t('appName')}</Text>

      {/* Card */}
      <View style={styles.card}>
        {/* Avatar */}
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{avatarLetter}</Text>
        </View>

        {/* Inviter info */}
        <Text style={styles.inviterName}>{inviteInfo?.inviterDisplayName}</Text>
        <Text style={styles.inviterUsername}>
          @{inviteInfo?.inviterUsername}
        </Text>

        <Text style={styles.tagline}>wants to be your friend on iBetcha</Text>

        {/* CTA */}
        {alreadyFriends ? (
          <View style={styles.alreadyFriendsBox}>
            <Text style={styles.alreadyFriendsText}>
              {t('inviteAlreadyFriends')}
            </Text>
          </View>
        ) : user ? (
          <TouchableOpacity
            style={[styles.button, accepting && styles.buttonDisabled]}
            onPress={handleAccept}
            disabled={accepting}
          >
            {accepting ? (
              <ActivityIndicator size="small" color={colors.textInverse} />
            ) : (
              <Text style={styles.buttonText}>{t('inviteAccept')}</Text>
            )}
          </TouchableOpacity>
        ) : (
          <TouchableOpacity style={styles.button} onPress={handleSignUp}>
            <Text style={styles.buttonText}>{t('inviteSignUpToConnect')}</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.background,
    padding: spacing.xl,
  },
  errorIcon: {
    fontSize: fontSize.xxxl,
    color: colors.danger,
    fontWeight: fontWeight.bold,
    marginBottom: spacing.md,
  },
  errorTitle: {
    fontSize: fontSize.lg,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.background,
    padding: spacing.xl,
  },
  appName: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.primary,
    marginBottom: spacing.xl,
  },
  card: {
    width: '100%',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xl,
    padding: spacing.xl,
    alignItems: 'center',
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 8,
    elevation: 4,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  avatarText: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  inviterName: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.bold,
    color: colors.text,
    marginBottom: spacing.xs,
    textAlign: 'center',
  },
  inviterUsername: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    marginBottom: spacing.md,
  },
  tagline: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    textAlign: 'center',
    marginBottom: spacing.xl,
  },
  button: {
    backgroundColor: colors.primary,
    borderRadius: borderRadius.lg,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xl,
    minWidth: 180,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.7,
  },
  buttonText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.semibold,
    color: colors.textInverse,
  },
  alreadyFriendsBox: {
    backgroundColor: colors.successLight,
    borderRadius: borderRadius.md,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  alreadyFriendsText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.success,
  },
});
