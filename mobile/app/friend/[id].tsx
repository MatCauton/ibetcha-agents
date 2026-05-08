import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  TouchableOpacity,
  Image,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Button } from '../../src/components/Button';
import { colors } from '../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';
import * as usersApi from '../../src/api/users';
import type { FriendProfile } from '../../src/types/domain';

export default function FriendProfileScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [profile, setProfile] = useState<FriendProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadProfile = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);

    try {
      const data = await usersApi.fetchUserProfile(id);
      setProfile(data);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load profile';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleChallenge = useCallback(() => {
    // Navigate to bet creation with this friend pre-selected
    // For the walking skeleton, we route to create and the user picks again
    router.push('/bet/create');
  }, [router]);

  if (isLoading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error || !profile) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error || 'Profile not found'}</Text>
        <Button title={t('retry')} onPress={loadProfile} />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.avatar}>
          {profile.avatarUrl ? (
            <Image
              source={{ uri: profile.avatarUrl }}
              style={styles.avatarImage}
            />
          ) : (
            <Text style={styles.avatarText}>
              {profile.displayName.charAt(0).toUpperCase()}
            </Text>
          )}
        </View>
        <Text style={styles.displayName}>{profile.displayName}</Text>
        <Text style={styles.username}>@{profile.username}</Text>
        {profile.bio && <Text style={styles.bio}>{profile.bio}</Text>}
      </View>

      {/* Stats */}
      <View style={styles.statsGrid}>
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{profile.stats.wins}</Text>
          <Text style={styles.statLabel}>{t('wins')}</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{profile.stats.losses}</Text>
          <Text style={styles.statLabel}>{t('losses')}</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>
            {Math.round(profile.stats.winRate * 100)}%
          </Text>
          <Text style={styles.statLabel}>{t('winRate')}</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>
            {profile.stats.currentStreak.count}
            {profile.stats.currentStreak.type === 'WIN' ? 'W' : 'L'}
          </Text>
          <Text style={styles.statLabel}>{t('streak')}</Text>
        </View>
      </View>

      {/* Head to Head */}
      <View style={styles.h2hCard}>
        <Text style={styles.h2hTitle}>{t('headToHead')}</Text>
        <View style={styles.h2hStats}>
          <View style={styles.h2hSide}>
            <Text style={styles.h2hValue}>{profile.headToHead.wins}</Text>
            <Text style={styles.h2hLabel}>{t('you')}</Text>
          </View>
          <Text style={styles.h2hDash}>-</Text>
          <View style={styles.h2hSide}>
            <Text style={styles.h2hValue}>{profile.headToHead.losses}</Text>
            <Text style={styles.h2hLabel}>{profile.displayName}</Text>
          </View>
        </View>
        <Text style={styles.h2hTotal}>
          {profile.headToHead.totalBets} bets together
        </Text>
      </View>

      {/* Challenge Button */}
      <View style={styles.challengeContainer}>
        <Button
          title={`Challenge ${profile.displayName}`}
          onPress={handleChallenge}
          fullWidth
          size="lg"
        />
      </View>

      {/* Active Bets */}
      {profile.activeBets.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{t('activeBetsTitle')}</Text>
          {profile.activeBets.map((bet) => (
            <TouchableOpacity
              key={bet.betId}
              style={styles.betRow}
              onPress={() => router.push(`/bet/${bet.betId}`)}
            >
              <Text style={styles.betDesc} numberOfLines={1}>
                {bet.description}
              </Text>
              <Text style={styles.betParticipants}>
                {bet.participants.join(', ')}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Recent Results */}
      {profile.recentResults.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{t('recentResults')}</Text>
          {profile.recentResults.map((result) => (
            <TouchableOpacity
              key={result.betId}
              style={styles.resultRow}
              onPress={() => router.push(`/bet/${result.betId}`)}
            >
              <View style={styles.resultInfo}>
                <Text style={styles.resultDesc} numberOfLines={1}>
                  {result.description}
                </Text>
                <Text style={styles.resultDate}>
                  {new Date(result.resolvedAt).toLocaleDateString()}
                </Text>
              </View>
              <Text
                style={[
                  styles.resultOutcome,
                  {
                    color:
                      result.outcome === 'WON'
                        ? colors.success
                        : colors.danger,
                  },
                ]}
              >
                {result.outcome}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}
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
    paddingBottom: spacing.md,
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

  // Head to Head
  h2hCard: {
    backgroundColor: colors.surface,
    marginHorizontal: spacing.md,
    marginTop: spacing.md,
    borderRadius: borderRadius.xl,
    padding: spacing.md,
    alignItems: 'center',
    elevation: 2,
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.5,
    shadowRadius: 2,
  },
  h2hTitle: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.textSecondary,
    textTransform: 'uppercase',
    marginBottom: spacing.sm,
  },
  h2hStats: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.lg,
  },
  h2hSide: {
    alignItems: 'center',
  },
  h2hValue: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },
  h2hLabel: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: 2,
  },
  h2hDash: {
    fontSize: fontSize.xxl,
    fontWeight: fontWeight.bold,
    color: colors.textTertiary,
  },
  h2hTotal: {
    fontSize: fontSize.xs,
    color: colors.textTertiary,
    marginTop: spacing.sm,
  },

  // Challenge
  challengeContainer: {
    paddingHorizontal: spacing.md,
    marginTop: spacing.lg,
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

  // Bet rows
  betRow: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    marginBottom: spacing.xs,
  },
  betDesc: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  betParticipants: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    marginTop: 2,
  },

  // Result rows
  resultRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    marginBottom: spacing.xs,
  },
  resultInfo: {
    flex: 1,
  },
  resultDesc: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  resultDate: {
    fontSize: fontSize.xs,
    color: colors.textTertiary,
    marginTop: 2,
  },
  resultOutcome: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    marginLeft: spacing.sm,
  },
});
