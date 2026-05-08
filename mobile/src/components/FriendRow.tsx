import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import type { Friend } from '../types/domain';
import { colors } from '../theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../theme/spacing';

interface FriendRowProps {
  friend: Friend;
  onPress: (friend: Friend) => void;
  rightAction?: React.ReactNode;
}

export function FriendRow({ friend, onPress, rightAction }: FriendRowProps) {
  return (
    <TouchableOpacity
      style={styles.row}
      activeOpacity={0.7}
      onPress={() => onPress(friend)}
    >
      <View style={styles.avatar}>
        {friend.avatarUrl ? (
          <Image
            source={{ uri: friend.avatarUrl }}
            style={styles.avatarImage}
          />
        ) : (
          <Text style={styles.avatarText}>
            {friend.displayName.charAt(0).toUpperCase()}
          </Text>
        )}
      </View>

      <View style={styles.info}>
        <Text style={styles.name} numberOfLines={1}>
          {friend.displayName}
        </Text>
        <Text style={styles.username}>@{friend.username}</Text>
      </View>

      <View style={styles.stats}>
        {friend.headToHead && (
          <Text style={styles.record}>
            {friend.headToHead.wins}W - {friend.headToHead.losses}L
          </Text>
        )}
      </View>

      {rightAction && <View style={styles.action}>{rightAction}</View>}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
    backgroundColor: colors.surface,
  },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  avatarImage: {
    width: 44,
    height: 44,
    borderRadius: 22,
  },
  avatarText: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  info: {
    flex: 1,
  },
  name: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  username: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: 2,
  },
  stats: {
    marginRight: spacing.sm,
  },
  record: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.primary,
  },
  action: {
    marginLeft: spacing.xs,
  },
});
