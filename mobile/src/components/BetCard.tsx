import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useRouter } from 'expo-router';
import type { Bet } from '../types/domain';
import { BetStatus, PendingAction } from '../types/domain';
import { colors } from '../theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../theme/spacing';
import { t } from '../i18n';

interface BetCardProps {
  bet: Bet;
}

function getStatusColor(status: BetStatus): string {
  switch (status) {
    case BetStatus.PENDING_ACCEPTANCE:
      return colors.statusPending;
    case BetStatus.ACTIVE:
      return colors.statusActive;
    case BetStatus.PENDING_APPROVAL:
    case BetStatus.PENDING_JURY_VERDICT:
      return colors.warning;
    case BetStatus.RESOLVED:
      return colors.statusResolved;
    case BetStatus.DISPUTED:
      return colors.statusDisputed;
    case BetStatus.EXPIRED:
    case BetStatus.CANCELLED:
      return colors.statusExpired;
    default:
      return colors.textSecondary;
  }
}

function getStatusLabel(status: BetStatus): string {
  switch (status) {
    case BetStatus.PENDING_ACCEPTANCE:
      return t('statusPendingAcceptance');
    case BetStatus.ACTIVE:
      return t('statusActive');
    case BetStatus.PENDING_APPROVAL:
      return t('statusPendingApproval');
    case BetStatus.PENDING_JURY_VERDICT:
      return t('statusPendingJury');
    case BetStatus.RESOLVED:
      return t('statusResolved');
    case BetStatus.DISPUTED:
      return t('statusDisputed');
    case BetStatus.EXPIRED:
      return t('statusExpired');
    case BetStatus.CANCELLED:
      return t('statusCancelled');
    default:
      return status;
  }
}

function getPendingActionLabel(action?: PendingAction): string | null {
  switch (action) {
    case PendingAction.ACCEPT_DECLINE:
      return 'Action needed: Accept or Decline';
    case PendingAction.APPROVE_OUTCOME:
      return 'Action needed: Approve outcome';
    case PendingAction.PENDING_JURY_VERDICT:
      return 'Waiting for jury verdict';
    default:
      return null;
  }
}

export function BetCard({ bet }: BetCardProps) {
  const router = useRouter();
  const statusColor = getStatusColor(bet.status);
  const statusLabel = getStatusLabel(bet.status);
  const actionLabel = getPendingActionLabel(bet.pendingAction);

  const participantNames = bet.participants
    .map((p) => p.displayName)
    .join(', ');

  const opponentDisplay =
    bet.participants.length === 1
      ? bet.participants[0].displayName
      : `${bet.participants.length} participants`;

  return (
    <TouchableOpacity
      style={styles.card}
      activeOpacity={0.7}
      onPress={() => router.push(`/bet/${bet.betId}`)}
    >
      <View style={styles.header}>
        <View style={styles.statusContainer}>
          <View style={[styles.statusDot, { backgroundColor: statusColor }]} />
          <Text style={[styles.statusText, { color: statusColor }]}>
            {statusLabel}
          </Text>
        </View>
        {bet.headToHead && (
          <Text style={styles.headToHead}>
            {bet.headToHead.wins}-{bet.headToHead.losses}
          </Text>
        )}
      </View>

      <Text style={styles.description} numberOfLines={2}>
        {bet.description}
      </Text>

      <View style={styles.metaRow}>
        <Text style={styles.opponent}>
          {bet.creator.displayName} {t('vs')} {opponentDisplay}
        </Text>
      </View>

      <View style={styles.footer}>
        <Text style={styles.stake} numberOfLines={1}>
          {bet.stake}
        </Text>
        {bet.createdAt && (
          <Text style={styles.date}>
            {new Date(bet.createdAt).toLocaleDateString()}
          </Text>
        )}
      </View>

      {actionLabel && (
        <View style={styles.actionBanner}>
          <Text style={styles.actionText}>{actionLabel}</Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xl,
    padding: spacing.md,
    marginHorizontal: spacing.md,
    marginBottom: spacing.sm,
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.8,
    shadowRadius: 4,
    elevation: 3,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: spacing.xs,
  },
  statusText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
    textTransform: 'uppercase',
  },
  headToHead: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },
  description: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
    marginBottom: spacing.sm,
    lineHeight: 22,
  },
  metaRow: {
    marginBottom: spacing.sm,
  },
  opponent: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  stake: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.medium,
    color: colors.primary,
    flex: 1,
  },
  date: {
    fontSize: fontSize.xs,
    color: colors.textTertiary,
  },
  actionBanner: {
    marginTop: spacing.sm,
    backgroundColor: colors.warningLight,
    borderRadius: borderRadius.md,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
  },
  actionText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.medium,
    color: colors.warning,
    textAlign: 'center',
  },
});
