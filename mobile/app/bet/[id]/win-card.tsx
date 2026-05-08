import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Share,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { getWinCard } from '../../../src/api/bets';
import { useAuthStore } from '../../../src/stores/authStore';
import { colors } from '../../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../../src/theme/spacing';
import { t } from '../../../src/i18n';
import type { WinCard } from '../../../src/types/domain';

export default function WinCardScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { user } = useAuthStore();
  const [winCard, setWinCard] = useState<WinCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getWinCard(id)
      .then(setWinCard)
      .catch(() => setError(t('serverError')))
      .finally(() => setLoading(false));
  }, [id]);

  const handleShare = async () => {
    if (!winCard) return;
    try {
      await Share.share({
        message: `I won the bet: ${winCard.description}. Stake: ${winCard.stake}. – iBetcha`,
      });
    } catch {
      Alert.alert(t('error'), 'Could not share the win card.');
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error || !winCard) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error ?? t('error')}</Text>
      </View>
    );
  }

  const isWinner = winCard.winner.userId === user?.id;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Card */}
      <View style={styles.card}>
        {/* Header band */}
        <View style={styles.cardHeader}>
          <Text style={styles.cardHeaderText}>iBetcha</Text>
        </View>

        {/* Winner callout */}
        <View style={styles.winnerSection}>
          <View style={styles.winnerBadge}>
            <Text style={styles.winnerBadgeText}>{t('winCardWinner')}</Text>
          </View>
          <Text style={styles.winnerName}>{winCard.winner.displayName}</Text>
          {isWinner && (
            <Text style={styles.winnerYouLabel}>({t('you')})</Text>
          )}
        </View>

        {/* Bet info */}
        <View style={styles.betInfo}>
          <Text style={styles.betDescription}>{winCard.description}</Text>
          <View style={styles.stakeRow}>
            <Text style={styles.stakeLabel}>{t('stake')}</Text>
            <Text style={styles.stakeValue}>{winCard.stake}</Text>
          </View>
        </View>

        {/* Participants */}
        <View style={styles.participantsList}>
          {winCard.allParticipants.map((p) => (
            <View key={p.userId} style={styles.participantRow}>
              <View style={styles.participantAvatar}>
                <Text style={styles.participantAvatarText}>
                  {p.displayName.charAt(0).toUpperCase()}
                </Text>
              </View>
              <Text style={styles.participantName}>
                {p.displayName}
                {p.userId === user?.id ? ` (${t('you')})` : ''}
              </Text>
              <View
                style={[
                  styles.outcomeTag,
                  p.won ? styles.outcomeTagWon : styles.outcomeTagLost,
                ]}
              >
                <Text
                  style={[
                    styles.outcomeTagText,
                    p.won ? styles.outcomeTagTextWon : styles.outcomeTagTextLost,
                  ]}
                >
                  {p.won ? t('winCardWinner') : t('winCardLoser')}
                </Text>
              </View>
            </View>
          ))}
        </View>

        {/* Head to Head record */}
        {winCard.headToHeadRecord && winCard.loser && (
          <View style={styles.h2hRow}>
            <Text style={styles.h2hText}>
              {isWinner
                ? `You're ${winCard.headToHeadRecord.wins}-${winCard.headToHeadRecord.losses} against ${winCard.loser.displayName} all time`
                : `You're ${winCard.headToHeadRecord.losses}-${winCard.headToHeadRecord.wins} against ${winCard.winner.displayName} all time`}
            </Text>
          </View>
        )}

        {/* Resolved date */}
        <Text style={styles.resolvedAt}>
          Resolved {new Date(winCard.resolvedAt).toLocaleDateString()}
        </Text>
      </View>

      {/* Share button */}
      <TouchableOpacity style={styles.shareButton} onPress={handleShare} activeOpacity={0.8}>
        <Text style={styles.shareButtonText}>{t('winCardShare')}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xxl,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.background,
  },
  errorText: {
    fontSize: fontSize.md,
    color: colors.danger,
    textAlign: 'center',
    marginHorizontal: spacing.lg,
  },

  // Card
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xl,
    overflow: 'hidden',
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 1,
    shadowRadius: 12,
    elevation: 6,
  },
  cardHeader: {
    backgroundColor: colors.primary,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  cardHeaderText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.textInverse,
    letterSpacing: 2,
    textTransform: 'uppercase',
  },

  // Winner
  winnerSection: {
    alignItems: 'center',
    paddingTop: spacing.xl,
    paddingBottom: spacing.lg,
    paddingHorizontal: spacing.md,
    backgroundColor: colors.primaryLight,
  },
  winnerBadge: {
    backgroundColor: colors.primary,
    borderRadius: borderRadius.full,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    marginBottom: spacing.sm,
  },
  winnerBadgeText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
    color: colors.textInverse,
    letterSpacing: 2,
  },
  winnerName: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
    textAlign: 'center',
  },
  winnerYouLabel: {
    fontSize: fontSize.md,
    color: colors.primaryDark,
    marginTop: spacing.xs,
  },

  // Bet info
  betInfo: {
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  betDescription: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
    lineHeight: 24,
    marginBottom: spacing.sm,
  },
  stakeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  stakeLabel: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textTransform: 'uppercase',
    fontWeight: fontWeight.semibold,
  },
  stakeValue: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },

  // Participants
  participantsList: {
    padding: spacing.md,
    gap: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  participantRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  participantAvatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: colors.surfaceSecondary,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  participantAvatarText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.textSecondary,
  },
  participantName: {
    flex: 1,
    fontSize: fontSize.sm,
    color: colors.text,
  },
  outcomeTag: {
    borderRadius: borderRadius.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
  },
  outcomeTagWon: {
    backgroundColor: colors.successLight,
  },
  outcomeTagLost: {
    backgroundColor: colors.dangerLight,
  },
  outcomeTagText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
  },
  outcomeTagTextWon: {
    color: colors.success,
  },
  outcomeTagTextLost: {
    color: colors.danger,
  },

  // Head to Head
  h2hRow: {
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    alignItems: 'center',
  },
  h2hText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: 'center',
    fontStyle: 'italic',
  },

  // Resolved date
  resolvedAt: {
    fontSize: fontSize.xs,
    color: colors.textTertiary,
    textAlign: 'center',
    padding: spacing.md,
  },

  // Share button
  shareButton: {
    marginTop: spacing.lg,
    backgroundColor: colors.primary,
    borderRadius: borderRadius.lg,
    paddingVertical: spacing.md,
    alignItems: 'center',
  },
  shareButtonText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.textInverse,
  },
});
