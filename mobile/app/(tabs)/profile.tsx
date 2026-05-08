import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
  ActivityIndicator,
  Image,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuthStore } from '../../src/stores/authStore';
import { Button } from '../../src/components/Button';
import { colors } from '../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';
import * as usersApi from '../../src/api/users';
import type { UserProfile } from '../../src/types/domain';

export default function ProfileScreen() {
  const router = useRouter();
  const { user, logout } = useAuthStore();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadProfile = useCallback(async (refresh = false) => {
    if (refresh) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError(null);

    try {
      const data = await usersApi.fetchOwnProfile();
      setProfile(data);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load profile';
      setError(message);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleLogout = useCallback(async () => {
    await logout();
    // RootLayout handles redirect
  }, [logout]);

  if (isLoading && !profile) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error && !profile) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
        <Button title={t('retry')} onPress={() => loadProfile()} />
      </View>
    );
  }

  const stats = profile?.stats;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl
          refreshing={isRefreshing}
          onRefresh={() => loadProfile(true)}
          tintColor={colors.primary}
        />
      }
    >
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.avatar}>
          {profile?.avatarUrl ? (
            <Image
              source={{ uri: profile.avatarUrl }}
              style={styles.avatarImage}
            />
          ) : (
            <Text style={styles.avatarText}>
              {(profile?.displayName || user?.email || '?')
                .charAt(0)
                .toUpperCase()}
            </Text>
          )}
        </View>
        <Text style={styles.displayName}>
          {profile?.displayName || user?.displayName || 'User'}
        </Text>
        {profile?.username && (
          <Text style={styles.username}>@{profile.username}</Text>
        )}
        {profile?.bio && <Text style={styles.bio}>{profile.bio}</Text>}
      </View>

      {/* Stats Grid */}
      {stats && (
        <View style={styles.statsGrid}>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{stats.wins}</Text>
            <Text style={styles.statLabel}>{t('wins')}</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{stats.losses}</Text>
            <Text style={styles.statLabel}>{t('losses')}</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statValue}>
              {Math.round(stats.winRate * 100)}%
            </Text>
            <Text style={styles.statLabel}>{t('winRate')}</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statValue}>
              {stats.currentStreak.count}
              {stats.currentStreak.type === 'WIN' ? 'W' : 'L'}
            </Text>
            <Text style={styles.statLabel}>{t('streak')}</Text>
          </View>
        </View>
      )}

      {/* Active Bets Count */}
      {stats && (
        <View style={styles.activeBetsRow}>
          <Text style={styles.activeBetsLabel}>{t('activeBets')}</Text>
          <Text style={styles.activeBetsValue}>{stats.activeBets}</Text>
        </View>
      )}

      {/* Top Rivals */}
      {profile?.topRivals && profile.topRivals.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{t('topRivals')}</Text>
          {profile.topRivals.map((rival) => (
            <TouchableOpacity
              key={rival.userId}
              style={styles.rivalRow}
              onPress={() => router.push(`/friend/${rival.userId}`)}
            >
              <View style={styles.rivalAvatar}>
                <Text style={styles.rivalAvatarText}>
                  {rival.displayName.charAt(0).toUpperCase()}
                </Text>
              </View>
              <View style={styles.rivalInfo}>
                <Text style={styles.rivalName}>{rival.displayName}</Text>
                <Text style={styles.rivalBets}>
                  {rival.totalBets} bets together
                </Text>
              </View>
              <Text style={styles.rivalRecord}>
                {rival.headToHead.wins}W - {rival.headToHead.losses}L
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Recent Bets */}
      {profile?.recentBets && profile.recentBets.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{t('recentBets')}</Text>
          {profile.recentBets.map((bet) => (
            <TouchableOpacity
              key={bet.betId}
              style={styles.recentBetRow}
              onPress={() => router.push(`/bet/${bet.betId}`)}
            >
              <View style={styles.recentBetInfo}>
                <Text style={styles.recentBetDesc} numberOfLines={1}>
                  {bet.description}
                </Text>
                <Text style={styles.recentBetMeta}>
                  vs {bet.opponentName}
                </Text>
              </View>
              <Text
                style={[
                  styles.recentBetOutcome,
                  {
                    color:
                      bet.outcome === 'WON' ? colors.success : colors.danger,
                  },
                ]}
              >
                {bet.outcome}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Logout */}
      <View style={styles.logoutSection}>
        <Button
          title={t('logout')}
          variant="outline"
          onPress={handleLogout}
          fullWidth
        />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    paddingBottom: spacing.xxl,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.background,
    gap: spacing.md,
  },
  errorText: {
    fontSize: fontSize.md,
    color: colors.danger,
    textAlign: 'center',
    marginHorizontal: spacing.lg,
  },

  // Header
  header: {
    alignItems: 'center',
    paddingTop: spacing.lg,
    paddingBottom: spacing.lg,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  avatarImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
  },
  avatarText: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  displayName: {
    fontSize: fontSize.xxl,
    fontWeight: fontWeight.bold,
    color: colors.text,
  },
  username: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    marginTop: 2,
  },
  bio: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: spacing.xs,
    textAlign: 'center',
    paddingHorizontal: spacing.xl,
  },

  // Stats
  statsGrid: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    marginHorizontal: spacing.md,
    borderRadius: borderRadius.xl,
    paddingVertical: spacing.md,
    elevation: 2,
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.5,
    shadowRadius: 2,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statValue: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.bold,
    color: colors.text,
  },
  statLabel: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    marginTop: 2,
  },
  statDivider: {
    width: 1,
    backgroundColor: colors.border,
  },

  activeBetsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginHorizontal: spacing.md,
    marginTop: spacing.md,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  activeBetsLabel: {
    fontSize: fontSize.md,
    color: colors.text,
    fontWeight: fontWeight.medium,
  },
  activeBetsValue: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },

  // Sections
  section: {
    marginTop: spacing.lg,
    paddingHorizontal: spacing.md,
  },
  sectionTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.semibold,
    color: colors.text,
    marginBottom: spacing.sm,
  },

  // Rivals
  rivalRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    marginBottom: spacing.xs,
  },
  rivalAvatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  rivalAvatarText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  rivalInfo: {
    flex: 1,
  },
  rivalName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  rivalBets: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
  },
  rivalRecord: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },

  // Recent bets
  recentBetRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    marginBottom: spacing.xs,
  },
  recentBetInfo: {
    flex: 1,
  },
  recentBetDesc: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  recentBetMeta: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    marginTop: 2,
  },
  recentBetOutcome: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
  },

  // Logout
  logoutSection: {
    marginTop: spacing.xl,
    paddingHorizontal: spacing.md,
  },
});
