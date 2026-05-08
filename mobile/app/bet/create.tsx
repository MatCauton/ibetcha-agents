import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Switch,
  Modal,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Input } from '../../src/components/Input';
import { Button } from '../../src/components/Button';
import { useBetStore } from '../../src/stores/betStore';
import { useFriendStore } from '../../src/stores/friendStore';
import { colors } from '../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';
import type { Friend } from '../../src/types/domain';

export default function CreateBetScreen() {
  const router = useRouter();
  const { createBet, isLoading } = useBetStore();
  const { friends, fetchFriends } = useFriendStore();

  // Step state
  const [step, setStep] = useState<'friend' | 'details'>('friend');
  const [selectedFriend, setSelectedFriend] = useState<Friend | null>(null);

  // Quick bet fields (always visible)
  const [description, setDescription] = useState('');
  const [stake, setStake] = useState('');

  // More options
  const [moreOptionsVisible, setMoreOptionsVisible] = useState(false);
  const [title, setTitle] = useState('');
  const [deadline, setDeadline] = useState('');
  const [selectedJury, setSelectedJury] = useState<Friend | null>(null);
  const [evidenceRequired, setEvidenceRequired] = useState(false);
  const [juryPickerVisible, setJuryPickerVisible] = useState(false);

  const [fieldErrors, setFieldErrors] = useState<{
    description?: string;
    stake?: string;
    deadline?: string;
  }>({});

  useEffect(() => {
    fetchFriends();
  }, []);

  const handleSelectFriend = useCallback((friend: Friend) => {
    setSelectedFriend(friend);
    // If the current jury selection was this friend, clear it
    setSelectedJury((prev) => (prev?.userId === friend.userId ? null : prev));
    setStep('details');
  }, []);

  function validate(): boolean {
    const errors: { description?: string; stake?: string; deadline?: string } = {};

    if (!description.trim()) {
      errors.description = 'Describe your bet';
    } else if (description.length > 500) {
      errors.description = 'Max 500 characters';
    }

    if (!stake.trim()) {
      errors.stake = 'Set the stakes';
    } else if (stake.length > 200) {
      errors.stake = 'Max 200 characters';
    }

    if (deadline.trim()) {
      const date = new Date(deadline.trim());
      if (isNaN(date.getTime())) {
        errors.deadline = 'Enter a valid date (YYYY-MM-DD)';
      } else if (date <= new Date()) {
        errors.deadline = 'Deadline must be in the future';
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSend() {
    if (!selectedFriend || !validate()) return;

    try {
      await createBet({
        description: description.trim(),
        stake: stake.trim(),
        participantIds: [selectedFriend.userId],
        title: title.trim() || null,
        deadline: deadline.trim() ? new Date(deadline.trim()).toISOString() : null,
        juryUserId: selectedJury?.userId ?? null,
        evidenceRequired,
      });
      router.back();
    } catch {
      Alert.alert(t('error'), 'Failed to create bet. Please try again.');
    }
  }

  // Friends eligible to be jury: friends who are NOT the selected participant
  const juryEligibleFriends = friends.filter(
    (f) => f.userId !== selectedFriend?.userId,
  );

  // ── Step: Friend picker ────────────────────────────────────────────────────

  if (step === 'friend') {
    return (
      <View style={styles.container}>
        <Text style={styles.stepTitle}>{t('pickFriend')}</Text>
        <FlatList
          data={friends}
          keyExtractor={(item) => item.userId}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.friendOption}
              activeOpacity={0.7}
              onPress={() => handleSelectFriend(item)}
            >
              <View style={styles.friendAvatar}>
                <Text style={styles.friendAvatarText}>
                  {item.displayName.charAt(0).toUpperCase()}
                </Text>
              </View>
              <View style={styles.friendInfo}>
                <Text style={styles.friendName}>{item.displayName}</Text>
                <Text style={styles.friendUsername}>@{item.username}</Text>
              </View>
              {item.headToHead && (
                <Text style={styles.friendRecord}>
                  {item.headToHead.wins}-{item.headToHead.losses}
                </Text>
              )}
            </TouchableOpacity>
          )}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.emptyFriends}>
              <Text style={styles.emptyText}>
                Add friends first to create a bet
              </Text>
            </View>
          }
        />
      </View>
    );
  }

  // ── Step: Bet details ──────────────────────────────────────────────────────

  return (
    <>
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView
          contentContainerStyle={styles.detailsContainer}
          keyboardShouldPersistTaps="handled"
        >
          {/* Selected friend header */}
          <TouchableOpacity
            style={styles.selectedFriend}
            onPress={() => setStep('friend')}
          >
            <View style={styles.selectedAvatar}>
              <Text style={styles.selectedAvatarText}>
                {selectedFriend?.displayName.charAt(0).toUpperCase()}
              </Text>
            </View>
            <View>
              <Text style={styles.selectedName}>
                vs {selectedFriend?.displayName}
              </Text>
              <Text style={styles.changeFriend}>Tap to change</Text>
            </View>
          </TouchableOpacity>

          {/* Quick bet fields */}
          <Input
            label={t('betDescription')}
            value={description}
            onChangeText={(text) => {
              setDescription(text);
              if (fieldErrors.description) {
                setFieldErrors((prev) => ({
                  ...prev,
                  description: undefined,
                }));
              }
            }}
            placeholder={t('betDescriptionPlaceholder')}
            multiline
            numberOfLines={2}
            maxLength={500}
            error={fieldErrors.description}
          />

          <Input
            label={t('betStake')}
            value={stake}
            onChangeText={(text) => {
              setStake(text);
              if (fieldErrors.stake) {
                setFieldErrors((prev) => ({ ...prev, stake: undefined }));
              }
            }}
            placeholder={t('betStakePlaceholder')}
            maxLength={200}
            error={fieldErrors.stake}
          />

          {/* More options toggle */}
          <TouchableOpacity
            style={styles.moreOptionsToggle}
            onPress={() => setMoreOptionsVisible((v) => !v)}
            activeOpacity={0.7}
          >
            <Text style={styles.moreOptionsToggleText}>
              {moreOptionsVisible ? 'Hide options' : `${t('moreOptions')} +`}
            </Text>
          </TouchableOpacity>

          {/* More options — expanded section */}
          {moreOptionsVisible && (
            <View style={styles.moreOptionsSection}>
              {/* Title */}
              <Input
                label="Title (optional)"
                value={title}
                onChangeText={setTitle}
                placeholder="e.g., Weekend Cycling Bet"
                maxLength={120}
              />

              {/* Deadline */}
              <Input
                label="Deadline (optional)"
                value={deadline}
                onChangeText={(text) => {
                  setDeadline(text);
                  if (fieldErrors.deadline) {
                    setFieldErrors((prev) => ({ ...prev, deadline: undefined }));
                  }
                }}
                placeholder="YYYY-MM-DD"
                keyboardType="numbers-and-punctuation"
                autoCapitalize="none"
                error={fieldErrors.deadline}
              />

              {/* Jury picker */}
              <View style={styles.optionRow}>
                <View style={styles.optionLabelGroup}>
                  <Text style={styles.optionLabel}>Jury (optional)</Text>
                  {selectedJury ? (
                    <Text style={styles.optionValue}>{selectedJury.displayName}</Text>
                  ) : (
                    <Text style={styles.optionPlaceholder}>None — majority vote</Text>
                  )}
                </View>
                <TouchableOpacity
                  style={styles.optionButton}
                  onPress={() => setJuryPickerVisible(true)}
                >
                  <Text style={styles.optionButtonText}>
                    {selectedJury ? 'Change' : 'Select'}
                  </Text>
                </TouchableOpacity>
              </View>

              {selectedJury && (
                <TouchableOpacity
                  onPress={() => setSelectedJury(null)}
                  style={styles.clearJury}
                >
                  <Text style={styles.clearJuryText}>Remove jury</Text>
                </TouchableOpacity>
              )}

              {/* Evidence required toggle */}
              <View style={styles.optionRow}>
                <View style={styles.optionLabelGroup}>
                  <Text style={styles.optionLabel}>Require evidence</Text>
                  <Text style={styles.optionPlaceholder}>
                    Winner must upload a photo/video
                  </Text>
                </View>
                <Switch
                  value={evidenceRequired}
                  onValueChange={setEvidenceRequired}
                  trackColor={{ false: colors.border, true: colors.primary }}
                  thumbColor={colors.surface}
                />
              </View>
            </View>
          )}

          {/* Send button */}
          <View style={styles.sendContainer}>
            <Button
              title={t('sendBet')}
              onPress={handleSend}
              loading={isLoading}
              fullWidth
              size="lg"
            />
          </View>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Jury picker modal */}
      <Modal
        visible={juryPickerVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setJuryPickerVisible(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setJuryPickerVisible(false)}
        >
          <View style={styles.modalSheet}>
            <Text style={styles.modalTitle}>Select Jury</Text>
            <Text style={styles.modalSubtitle}>
              Choose a friend who is NOT a participant
            </Text>
            {juryEligibleFriends.length === 0 ? (
              <Text style={styles.modalEmpty}>
                No eligible friends (you need friends who are not in this bet)
              </Text>
            ) : (
              <FlatList
                data={juryEligibleFriends}
                keyExtractor={(item) => item.userId}
                renderItem={({ item }) => (
                  <TouchableOpacity
                    style={[
                      styles.modalOption,
                      selectedJury?.userId === item.userId && styles.modalOptionSelected,
                    ]}
                    onPress={() => {
                      setSelectedJury(item);
                      setJuryPickerVisible(false);
                    }}
                    activeOpacity={0.7}
                  >
                    <View style={styles.modalOptionAvatar}>
                      <Text style={styles.modalOptionAvatarText}>
                        {item.displayName.charAt(0).toUpperCase()}
                      </Text>
                    </View>
                    <View>
                      <Text style={styles.modalOptionText}>{item.displayName}</Text>
                      <Text style={styles.modalOptionUsername}>@{item.username}</Text>
                    </View>
                    {selectedJury?.userId === item.userId && (
                      <Text style={styles.modalOptionCheck}>Selected</Text>
                    )}
                  </TouchableOpacity>
                )}
              />
            )}
            <TouchableOpacity
              style={styles.modalCancel}
              onPress={() => setJuryPickerVisible(false)}
            >
              <Text style={styles.modalCancelText}>{t('cancel')}</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  stepTitle: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.semibold,
    color: colors.text,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
  },
  listContent: {
    paddingBottom: spacing.lg,
  },

  // Friend selection
  friendOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  friendAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  friendAvatarText: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  friendInfo: {
    flex: 1,
  },
  friendName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  friendUsername: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  friendRecord: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.primary,
  },
  emptyFriends: {
    padding: spacing.xl,
    alignItems: 'center',
  },
  emptyText: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    textAlign: 'center',
  },

  // Details form
  detailsContainer: {
    padding: spacing.md,
    paddingBottom: spacing.xxl,
  },
  selectedFriend: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    marginBottom: spacing.lg,
  },
  selectedAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  selectedAvatarText: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  selectedName: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.semibold,
    color: colors.text,
  },
  changeFriend: {
    fontSize: fontSize.xs,
    color: colors.primary,
  },

  // More options toggle
  moreOptionsToggle: {
    paddingVertical: spacing.sm,
    alignItems: 'center',
    marginTop: spacing.xs,
    marginBottom: spacing.xs,
  },
  moreOptionsToggleText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.primary,
  },

  // More options section
  moreOptionsSection: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.md,
    gap: spacing.sm,
  },

  // Option rows (jury, evidence toggle)
  optionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
  },
  optionLabelGroup: {
    flex: 1,
    marginRight: spacing.sm,
  },
  optionLabel: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  optionValue: {
    fontSize: fontSize.sm,
    color: colors.primary,
    marginTop: 2,
  },
  optionPlaceholder: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    marginTop: 2,
  },
  optionButton: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    borderColor: colors.primary,
  },
  optionButtonText: {
    fontSize: fontSize.sm,
    color: colors.primary,
    fontWeight: fontWeight.medium,
  },

  // Clear jury
  clearJury: {
    alignSelf: 'flex-end',
    paddingVertical: 2,
  },
  clearJuryText: {
    fontSize: fontSize.xs,
    color: colors.danger,
  },

  // Send
  sendContainer: {
    marginTop: spacing.md,
  },

  // Modal
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
    maxHeight: '70%',
  },
  modalTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.text,
    textAlign: 'center',
    paddingHorizontal: spacing.md,
    marginBottom: spacing.xs,
  },
  modalSubtitle: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: 'center',
    paddingHorizontal: spacing.md,
    marginBottom: spacing.md,
  },
  modalEmpty: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xl,
  },
  modalOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
  },
  modalOptionSelected: {
    backgroundColor: colors.primaryLight + '33',
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
  modalOptionUsername: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
  },
  modalOptionCheck: {
    marginLeft: 'auto',
    fontSize: fontSize.xs,
    fontWeight: fontWeight.semibold,
    color: colors.primary,
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
