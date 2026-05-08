import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  TouchableOpacity,
  Modal,
  FlatList,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { useBetStore } from '../../../src/stores/betStore';
import { useAuthStore } from '../../../src/stores/authStore';
import { Button } from '../../../src/components/Button';
import { colors } from '../../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../../src/theme/spacing';
import { t } from '../../../src/i18n';
import { BetStatus } from '../../../src/types/domain';
import type { Participant } from '../../../src/types/domain';

export default function BetDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const {
    currentBet,
    isLoading,
    error,
    fetchBetById,
    acceptBet,
    declineBet,
    completeBet,
    voteBet,
    concedeBet,
    submitJuryVerdict,
    clearCurrentBet,
    evidence,
    evidenceLoading,
    fetchEvidence,
    uploadEvidence,
  } = useBetStore();
  const { user } = useAuthStore();
  const [actionLoading, setActionLoading] = useState(false);
  const [winnerPickerVisible, setWinnerPickerVisible] = useState(false);
  const [juryWinnerPickerVisible, setJuryWinnerPickerVisible] = useState(false);

  useEffect(() => {
    if (id) {
      fetchBetById(id);
      fetchEvidence(id);
    }
    return () => clearCurrentBet();
  }, [id]);

  const handleAction = useCallback(
    async (action: () => Promise<void>, confirmMessage?: string) => {
      if (confirmMessage) {
        Alert.alert(t('confirm'), confirmMessage, [
          { text: t('cancel'), style: 'cancel' },
          {
            text: t('confirm'),
            onPress: async () => {
              setActionLoading(true);
              try {
                await action();
                if (id) fetchBetById(id);
              } catch {
                Alert.alert(t('error'), 'Action failed. Please try again.');
              } finally {
                setActionLoading(false);
              }
            },
          },
        ]);
      } else {
        setActionLoading(true);
        try {
          await action();
          if (id) fetchBetById(id);
        } catch {
          Alert.alert(t('error'), 'Action failed. Please try again.');
        } finally {
          setActionLoading(false);
        }
      }
    },
    [id, fetchBetById],
  );

  const handleAddEvidence = useCallback(async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images', 'videos'],
      allowsEditing: false,
      quality: 0.8,
    });

    if (result.canceled || !result.assets.length || !id) return;

    const asset = result.assets[0];
    const uri = asset.uri;
    const fileName = asset.fileName ?? uri.split('/').pop() ?? 'upload';
    const mimeType = asset.mimeType ?? 'image/jpeg';

    try {
      await uploadEvidence(id, uri, mimeType, fileName);
      Alert.alert(t('evidenceUploaded'), t('evidenceUploadSuccess'));
    } catch {
      Alert.alert(t('error'), t('evidenceUploadError'));
    }
  }, [id, uploadEvidence]);

  const handleSelectWinner = useCallback(() => {
    if (!currentBet || !id) return;
    setWinnerPickerVisible(true);
  }, [currentBet, id]);

  const handleJuryApprove = useCallback(() => {
    if (!currentBet || !id) return;
    setJuryWinnerPickerVisible(true);
  }, [currentBet, id]);

  const handleJuryReject = useCallback(() => {
    if (!id) return;
    Alert.alert(
      'Reject Outcome',
      'Rejecting will return this bet to ACTIVE status. Are you sure?',
      [
        { text: t('cancel'), style: 'cancel' },
        {
          text: 'Reject',
          style: 'destructive',
          onPress: () =>
            handleAction(
              () => submitJuryVerdict(id, false, undefined),
            ),
        },
      ],
    );
  }, [id, handleAction, submitJuryVerdict]);

  const handleWinnerSelected = useCallback(
    (winnerId: string, winnerName: string) => {
      setWinnerPickerVisible(false);
      if (!id) return;
      Alert.alert(
        t('confirm'),
        `Declare ${winnerName} as the winner?`,
        [
          { text: t('cancel'), style: 'cancel' },
          {
            text: t('confirm'),
            onPress: () => handleAction(() => completeBet(id, winnerId)),
          },
        ],
      );
    },
    [id, handleAction, completeBet],
  );

  const handleJuryWinnerSelected = useCallback(
    (winnerId: string, winnerName: string) => {
      setJuryWinnerPickerVisible(false);
      if (!id) return;
      Alert.alert(
        t('confirm'),
        `Approve outcome and declare ${winnerName} as the winner?`,
        [
          { text: t('cancel'), style: 'cancel' },
          {
            text: 'Approve',
            onPress: () => handleAction(() => submitJuryVerdict(id, true, winnerId)),
          },
        ],
      );
    },
    [id, handleAction, submitJuryVerdict],
  );

  if (isLoading || !currentBet) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
        <Button title={t('retry')} onPress={() => id && fetchBetById(id)} />
      </View>
    );
  }

  const isCreator = currentBet.creator.userId === user?.id;
  const isParticipant = currentBet.participants.some(
    (p) => p.userId === user?.id,
  );
  const isJury =
    currentBet.jury != null && currentBet.jury.userId === user?.id;
  const isInvolved = isCreator || isParticipant;

  const canAddEvidence =
    (currentBet.status === BetStatus.ACTIVE ||
      currentBet.status === BetStatus.PENDING_APPROVAL) &&
    isInvolved;

  const canViewWinCard =
    currentBet.status === BetStatus.RESOLVED && isInvolved;

  const allParticipants: Array<{ id: string; name: string }> = [
    { id: currentBet.creator.userId, name: currentBet.creator.displayName },
    ...currentBet.participants.map((p: Participant) => ({
      id: p.userId,
      name: p.displayName,
    })),
  ];

  return (
    <>
      <ScrollView style={styles.container} contentContainerStyle={styles.content}>
        {/* Jury banner */}
        {isJury && currentBet.status === BetStatus.PENDING_JURY_VERDICT && (
          <View style={styles.juryBanner}>
            <Text style={styles.juryBannerText}>You are the jury for this bet</Text>
          </View>
        )}

        {/* Status Banner */}
        <View
          style={[
            styles.statusBanner,
            { backgroundColor: getStatusBg(currentBet.status) },
          ]}
        >
          <Text
            style={[
              styles.statusText,
              { color: getStatusFg(currentBet.status) },
            ]}
          >
            {getStatusLabel(currentBet.status)}
          </Text>
        </View>

        {/* Description */}
        <View style={styles.section}>
          <Text style={styles.description}>{currentBet.description}</Text>
          {currentBet.title && (
            <Text style={styles.title}>{currentBet.title}</Text>
          )}
        </View>

        {/* Stake */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>{t('stake')}</Text>
          <Text style={styles.stakeValue}>{currentBet.stake}</Text>
        </View>

        {/* Creator */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>{t('createdBy')}</Text>
          <View style={styles.personRow}>
            <View style={styles.personAvatar}>
              <Text style={styles.personAvatarText}>
                {currentBet.creator.displayName.charAt(0).toUpperCase()}
              </Text>
            </View>
            <Text style={styles.personName}>
              {currentBet.creator.displayName}
              {isCreator ? ` (${t('you')})` : ''}
            </Text>
          </View>
        </View>

        {/* Participants */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>{t('participants')}</Text>
          {currentBet.participants.map((p) => (
            <View key={p.userId} style={styles.personRow}>
              <View style={styles.personAvatar}>
                <Text style={styles.personAvatarText}>
                  {p.displayName.charAt(0).toUpperCase()}
                </Text>
              </View>
              <View style={styles.personInfo}>
                <Text style={styles.personName}>
                  {p.displayName}
                  {p.userId === user?.id ? ` (${t('you')})` : ''}
                </Text>
                <Text
                  style={[
                    styles.participantStatus,
                    {
                      color:
                        p.status === 'ACCEPTED'
                          ? colors.success
                          : p.status === 'DECLINED'
                            ? colors.danger
                            : colors.warning,
                    },
                  ]}
                >
                  {p.status}
                </Text>
              </View>
            </View>
          ))}
        </View>

        {/* Jury */}
        {currentBet.jury && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>Jury</Text>
            <View style={styles.personRow}>
              <View style={styles.personAvatar}>
                <Text style={styles.personAvatarText}>
                  {currentBet.jury.displayName.charAt(0).toUpperCase()}
                </Text>
              </View>
              <Text style={styles.personName}>
                {currentBet.jury.displayName}
                {isJury ? ` (${t('you')})` : ''}
              </Text>
            </View>
          </View>
        )}

        {/* Head to Head */}
        {currentBet.headToHead && (
          <View style={styles.h2hSection}>
            <Text style={styles.sectionLabel}>{t('headToHead')}</Text>
            <Text style={styles.h2hValue}>
              {currentBet.headToHead.wins} {t('wins')} -{' '}
              {currentBet.headToHead.losses} {t('losses')}
            </Text>
          </View>
        )}

        {/* Outcome */}
        {currentBet.outcome && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>{t('outcome')}</Text>
            <Text style={styles.outcomeText}>
              Winner declared: {currentBet.outcome.winnerId === user?.id ? t('you') : 'Opponent'}
            </Text>
            <Text style={styles.outcomeStatus}>
              Status: {currentBet.outcome.approvalStatus}
            </Text>
            {currentBet.outcome.votes.length > 0 && (
              <View style={styles.votesContainer}>
                {currentBet.outcome.votes.map((v, i) => (
                  <Text key={i} style={styles.voteItem}>
                    {v.vote}
                  </Text>
                ))}
              </View>
            )}
          </View>
        )}

        {/* Evidence section */}
        <View style={styles.section}>
          <View style={styles.evidenceHeader}>
            <Text style={styles.sectionLabel}>{t('evidence')}</Text>
            {canAddEvidence && (
              evidenceLoading ? (
                <ActivityIndicator size="small" color={colors.primary} />
              ) : (
                <TouchableOpacity
                  style={styles.addEvidenceButton}
                  onPress={handleAddEvidence}
                  activeOpacity={0.7}
                >
                  <Text style={styles.addEvidenceText}>{t('addEvidence')}</Text>
                </TouchableOpacity>
              )
            )}
          </View>
          {evidence.length === 0 ? (
            <Text style={styles.evidenceEmpty}>{t('evidenceEmpty')}</Text>
          ) : (
            evidence.map((ev) => (
              <View key={ev.evidenceId} style={styles.evidenceItem}>
                <Text style={styles.evidenceFileName}>{ev.fileName}</Text>
                <Text style={styles.evidenceContentType}>{ev.contentType}</Text>
                <Text style={styles.evidenceDate}>
                  {new Date(ev.uploadedAt).toLocaleDateString()}
                </Text>
              </View>
            ))
          )}
        </View>

        {/* Deadline */}
        {currentBet.acceptanceDeadline && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>{t('deadline')}</Text>
            <Text style={styles.deadlineValue}>
              {new Date(currentBet.acceptanceDeadline).toLocaleString()}
            </Text>
          </View>
        )}

        {/* Actions */}
        <View style={styles.actions}>
          {/* PENDING_ACCEPTANCE: Accept/Decline for non-creator participant */}
          {currentBet.status === BetStatus.PENDING_ACCEPTANCE &&
            isParticipant &&
            !isCreator && (
              <View style={styles.actionRow}>
                <Button
                  title={t('accept')}
                  onPress={() =>
                    handleAction(() => acceptBet(id!), 'Accept this bet?')
                  }
                  loading={actionLoading}
                  style={styles.actionButton}
                />
                <Button
                  title={t('decline')}
                  variant="danger"
                  onPress={() =>
                    handleAction(() => declineBet(id!), 'Decline this bet?')
                  }
                  loading={actionLoading}
                  style={styles.actionButton}
                />
              </View>
            )}

          {/* ACTIVE: Mark Complete */}
          {currentBet.status === BetStatus.ACTIVE &&
            isInvolved && (
              <Button
                title={t('markComplete')}
                onPress={handleSelectWinner}
                loading={actionLoading}
                fullWidth
                size="lg"
              />
            )}

          {/* PENDING_APPROVAL: Approve/Dispute (non-declarer participants) */}
          {currentBet.status === BetStatus.PENDING_APPROVAL &&
            isParticipant &&
            currentBet.outcome &&
            currentBet.outcome.declaredBy !== user?.id && (
              <View style={styles.actionRow}>
                <Button
                  title={t('approve')}
                  onPress={() =>
                    handleAction(
                      () => voteBet(id!, 'APPROVE'),
                      'Approve the outcome?',
                    )
                  }
                  loading={actionLoading}
                  style={styles.actionButton}
                />
                <Button
                  title={t('dispute')}
                  variant="danger"
                  onPress={() =>
                    handleAction(
                      () => voteBet(id!, 'DISPUTE'),
                      'Dispute the outcome?',
                    )
                  }
                  loading={actionLoading}
                  style={styles.actionButton}
                />
              </View>
            )}

          {/* PENDING_JURY_VERDICT: Jury Approve/Reject */}
          {currentBet.status === BetStatus.PENDING_JURY_VERDICT && isJury && (
            <View style={styles.juryActions}>
              <Text style={styles.juryActionHint}>
                Review the bet and declare who won.
              </Text>
              <View style={styles.actionRow}>
                <Button
                  title="Approve"
                  onPress={handleJuryApprove}
                  loading={actionLoading}
                  style={styles.actionButton}
                />
                <Button
                  title="Reject"
                  variant="danger"
                  onPress={handleJuryReject}
                  loading={actionLoading}
                  style={styles.actionButton}
                />
              </View>
            </View>
          )}

          {/* DISPUTED: Concede */}
          {currentBet.status === BetStatus.DISPUTED &&
            isInvolved && (
              <Button
                title={t('concede')}
                variant="outline"
                onPress={() =>
                  handleAction(
                    () => concedeBet(id!),
                    'Concede this bet? The other party wins.',
                  )
                }
                loading={actionLoading}
                fullWidth
                size="lg"
              />
            )}

          {/* RESOLVED: Show result + Win Card */}
          {currentBet.status === BetStatus.RESOLVED && currentBet.outcome && (
            <View style={styles.resolvedBanner}>
              <Text style={styles.resolvedText}>
                {currentBet.outcome.winnerId === user?.id
                  ? 'You won!'
                  : 'You lost'}
              </Text>
            </View>
          )}

          {canViewWinCard && (
            <Button
              title={t('viewWinCard')}
              variant="outline"
              onPress={() => router.push(`/bet/${id}/win-card`)}
              fullWidth
              size="lg"
              style={styles.winCardButton}
            />
          )}
        </View>
      </ScrollView>

      {/* Winner picker modal — for Mark Complete */}
      <WinnerPickerModal
        visible={winnerPickerVisible}
        people={allParticipants}
        currentUserId={user?.id}
        onSelect={handleWinnerSelected}
        onClose={() => setWinnerPickerVisible(false)}
        title={t('selectWinner')}
      />

      {/* Winner picker modal — for Jury Approve */}
      <WinnerPickerModal
        visible={juryWinnerPickerVisible}
        people={allParticipants}
        currentUserId={user?.id}
        onSelect={handleJuryWinnerSelected}
        onClose={() => setJuryWinnerPickerVisible(false)}
        title="Who won?"
      />
    </>
  );
}

// ── Winner Picker Modal ───────────────────────────────────────────────────────

interface WinnerPickerModalProps {
  visible: boolean;
  people: Array<{ id: string; name: string }>;
  currentUserId: string | undefined;
  onSelect: (id: string, name: string) => void;
  onClose: () => void;
  title: string;
}

function WinnerPickerModal({
  visible,
  people,
  currentUserId,
  onSelect,
  onClose,
  title,
}: WinnerPickerModalProps) {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={onClose}>
        <View style={styles.modalSheet}>
          <Text style={styles.modalTitle}>{title}</Text>
          <FlatList
            data={people}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={styles.modalOption}
                onPress={() => onSelect(item.id, item.name)}
                activeOpacity={0.7}
              >
                <View style={styles.modalOptionAvatar}>
                  <Text style={styles.modalOptionAvatarText}>
                    {item.name.charAt(0).toUpperCase()}
                  </Text>
                </View>
                <Text style={styles.modalOptionText}>
                  {item.id === currentUserId ? `${item.name} (${t('you')})` : item.name}
                </Text>
              </TouchableOpacity>
            )}
          />
          <TouchableOpacity style={styles.modalCancel} onPress={onClose}>
            <Text style={styles.modalCancelText}>{t('cancel')}</Text>
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    </Modal>
  );
}

// ── Status helpers ────────────────────────────────────────────────────────────

function getStatusLabel(status: BetStatus): string {
  const labels: Record<BetStatus, string> = {
    [BetStatus.PENDING_ACCEPTANCE]: 'Pending Acceptance',
    [BetStatus.ACTIVE]: 'Active',
    [BetStatus.PENDING_APPROVAL]: 'Pending Approval',
    [BetStatus.PENDING_JURY_VERDICT]: 'Awaiting Jury',
    [BetStatus.RESOLVED]: 'Resolved',
    [BetStatus.DISPUTED]: 'Disputed',
    [BetStatus.EXPIRED]: 'Expired',
    [BetStatus.CANCELLED]: 'Cancelled',
  };
  return labels[status] ?? status;
}

function getStatusBg(status: BetStatus): string {
  switch (status) {
    case BetStatus.PENDING_ACCEPTANCE:
      return colors.warningLight;
    case BetStatus.ACTIVE:
      return colors.primaryLight;
    case BetStatus.PENDING_APPROVAL:
    case BetStatus.PENDING_JURY_VERDICT:
      return colors.warningLight;
    case BetStatus.RESOLVED:
      return colors.successLight;
    case BetStatus.DISPUTED:
      return colors.dangerLight;
    default:
      return colors.surfaceSecondary;
  }
}

function getStatusFg(status: BetStatus): string {
  switch (status) {
    case BetStatus.PENDING_ACCEPTANCE:
      return colors.warning;
    case BetStatus.ACTIVE:
      return colors.primaryDark;
    case BetStatus.PENDING_APPROVAL:
    case BetStatus.PENDING_JURY_VERDICT:
      return colors.warning;
    case BetStatus.RESOLVED:
      return colors.success;
    case BetStatus.DISPUTED:
      return colors.danger;
    default:
      return colors.textSecondary;
  }
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
    gap: spacing.md,
  },
  errorText: {
    fontSize: fontSize.md,
    color: colors.danger,
    textAlign: 'center',
    marginHorizontal: spacing.lg,
  },

  // Jury banner
  juryBanner: {
    backgroundColor: colors.primaryLight,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginBottom: spacing.sm,
    alignItems: 'center',
  },
  juryBannerText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.primaryDark,
  },

  // Status
  statusBanner: {
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginBottom: spacing.md,
    alignItems: 'center',
  },
  statusText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    textTransform: 'uppercase',
  },

  // Sections
  section: {
    marginBottom: spacing.md,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
  },
  sectionLabel: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
    color: colors.textSecondary,
    textTransform: 'uppercase',
    marginBottom: spacing.xs,
  },
  description: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.medium,
    color: colors.text,
    lineHeight: 26,
  },
  title: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  stakeValue: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.semibold,
    color: colors.primary,
  },

  // People
  personRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing.xs,
  },
  personAvatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  personAvatarText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  personInfo: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  personName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  participantStatus: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
  },

  // Head to Head
  h2hSection: {
    marginBottom: spacing.md,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    alignItems: 'center',
  },
  h2hValue: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },

  // Outcome
  outcomeText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  outcomeStatus: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  votesContainer: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.sm,
  },
  voteItem: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.medium,
    color: colors.textSecondary,
  },

  // Deadline
  deadlineValue: {
    fontSize: fontSize.md,
    color: colors.text,
  },

  // Evidence
  evidenceHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  addEvidenceButton: {
    backgroundColor: colors.primary,
    borderRadius: borderRadius.md,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
  },
  addEvidenceText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
    color: colors.textInverse,
  },
  evidenceEmpty: {
    fontSize: fontSize.sm,
    color: colors.textTertiary,
    fontStyle: 'italic',
    marginTop: spacing.xs,
  },
  evidenceItem: {
    marginTop: spacing.sm,
    paddingTop: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  evidenceFileName: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  evidenceContentType: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    marginTop: 2,
  },
  evidenceDate: {
    fontSize: fontSize.xs,
    color: colors.textTertiary,
    marginTop: 2,
  },

  // Actions
  actions: {
    marginTop: spacing.lg,
  },
  actionRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  actionButton: {
    flex: 1,
  },

  // Jury actions
  juryActions: {
    gap: spacing.sm,
  },
  juryActionHint: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: 'center',
    marginBottom: spacing.xs,
  },

  // Resolved
  resolvedBanner: {
    backgroundColor: colors.successLight,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  resolvedText: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.bold,
    color: colors.success,
  },
  winCardButton: {
    marginTop: spacing.sm,
  },

  // Winner picker modal
  modalOverlay: {
    flex: 1,
    justifyContent: 'flex-end',
    backgroundColor: colors.overlay,
  },
  modalSheet: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: borderRadius.xl,
    borderTopRightRadius: borderRadius.xl,
    paddingTop: spacing.md,
    paddingBottom: spacing.xl,
    maxHeight: '60%',
  },
  modalTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.text,
    textAlign: 'center',
    paddingHorizontal: spacing.md,
    paddingBottom: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    marginBottom: spacing.xs,
  },
  modalOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
  },
  modalOptionAvatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  modalOptionAvatarText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  modalOptionText: {
    fontSize: fontSize.md,
    color: colors.text,
  },
  modalCancel: {
    marginTop: spacing.sm,
    paddingVertical: spacing.sm,
    marginHorizontal: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    alignItems: 'center',
  },
  modalCancelText: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
  },
});
