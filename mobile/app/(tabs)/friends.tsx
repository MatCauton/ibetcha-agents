import React, { useCallback, useEffect, useState, useRef } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  TextInput,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useFriendStore } from '../../src/stores/friendStore';
import { FriendRow } from '../../src/components/FriendRow';
import { Button } from '../../src/components/Button';
import { EmptyState } from '../../src/components/EmptyState';
import { colors } from '../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';
import type { Friend } from '../../src/types/domain';
import { FriendshipStatus } from '../../src/types/domain';

export default function FriendsScreen() {
  const router = useRouter();
  const {
    friends,
    friendRequests,
    searchResults,
    isLoading,
    isRefreshing,
    isSearching,
    fetchFriends,
    fetchFriendRequests,
    searchUsers,
    sendFriendRequest,
    acceptRequest,
    declineRequest,
    clearSearch,
  } = useFriendStore();

  const [searchQuery, setSearchQuery] = useState('');
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    fetchFriends();
    fetchFriendRequests();
  }, []);

  const handleSearch = useCallback(
    (text: string) => {
      setSearchQuery(text);

      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }

      if (text.length < 2) {
        clearSearch();
        return;
      }

      searchTimeoutRef.current = setTimeout(() => {
        searchUsers(text);
      }, 300);
    },
    [searchUsers, clearSearch],
  );

  const handleRefresh = useCallback(() => {
    fetchFriends(true);
    fetchFriendRequests();
  }, [fetchFriends, fetchFriendRequests]);

  const handleFriendPress = useCallback(
    (friend: Friend) => {
      router.push(`/friend/${friend.userId}`);
    },
    [router],
  );

  const isSearchActive = searchQuery.length >= 2;

  if (isLoading && friends.length === 0) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Search bar */}
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder={t('searchFriends')}
          placeholderTextColor={colors.textTertiary}
          value={searchQuery}
          onChangeText={handleSearch}
          autoCapitalize="none"
          autoCorrect={false}
        />
        {isSearching && (
          <ActivityIndicator
            size="small"
            color={colors.primary}
            style={styles.searchSpinner}
          />
        )}
      </View>

      {isSearchActive ? (
        /* Search results */
        <FlatList
          data={searchResults}
          keyExtractor={(item) => item.userId}
          renderItem={({ item }) => (
            <View style={styles.searchRow}>
              <View style={styles.searchRowInfo}>
                <View style={styles.searchAvatar}>
                  <Text style={styles.searchAvatarText}>
                    {item.displayName.charAt(0).toUpperCase()}
                  </Text>
                </View>
                <View>
                  <Text style={styles.searchName}>{item.displayName}</Text>
                  <Text style={styles.searchUsername}>@{item.username}</Text>
                </View>
              </View>
              {item.friendshipStatus === FriendshipStatus.FRIENDS ? (
                <Text style={styles.friendsBadge}>Friends</Text>
              ) : item.friendshipStatus === FriendshipStatus.PENDING_SENT ? (
                <Text style={styles.pendingBadge}>Pending</Text>
              ) : (
                <Button
                  title={t('addFriend')}
                  size="sm"
                  onPress={() => sendFriendRequest(item.userId)}
                />
              )}
            </View>
          )}
          ListEmptyComponent={
            !isSearching ? (
              <View style={styles.noResults}>
                <Text style={styles.noResultsText}>{t('noResults')}</Text>
              </View>
            ) : null
          }
          contentContainerStyle={styles.listContent}
        />
      ) : (
        /* Friends list with pending requests */
        <FlatList
          data={friends}
          keyExtractor={(item) => item.userId}
          renderItem={({ item }) => (
            <FriendRow friend={item} onPress={handleFriendPress} />
          )}
          refreshControl={
            <RefreshControl
              refreshing={isRefreshing}
              onRefresh={handleRefresh}
              tintColor={colors.primary}
            />
          }
          ListHeaderComponent={
            friendRequests.length > 0 ? (
              <View style={styles.requestsSection}>
                <Text style={styles.sectionTitle}>
                  {t('pendingRequests')} ({friendRequests.length})
                </Text>
                {friendRequests.map((req) => (
                  <View key={req.requestId} style={styles.requestRow}>
                    <View style={styles.requestAvatar}>
                      <Text style={styles.requestAvatarText}>
                        {req.fromUser.displayName.charAt(0).toUpperCase()}
                      </Text>
                    </View>
                    <View style={styles.requestInfo}>
                      <Text style={styles.requestName}>
                        {req.fromUser.displayName}
                      </Text>
                      <Text style={styles.requestUsername}>
                        @{req.fromUser.username}
                      </Text>
                    </View>
                    <View style={styles.requestActions}>
                      <Button
                        title={t('accept')}
                        size="sm"
                        onPress={() => acceptRequest(req.requestId)}
                      />
                      <TouchableOpacity
                        style={styles.declineButton}
                        onPress={() => declineRequest(req.requestId)}
                      >
                        <Text style={styles.declineText}>{t('decline')}</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                ))}
                <View style={styles.separator} />
              </View>
            ) : null
          }
          ListEmptyComponent={
            <EmptyState
              title={t('noFriends')}
              description={t('noFriendsDescription')}
              actionLabel={t('inviteFriend')}
              onAction={() => {
                // Invite flow placeholder
              }}
            />
          }
          contentContainerStyle={
            friends.length === 0 ? styles.emptyContainer : styles.listContent
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.background,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    backgroundColor: colors.background,
  },
  searchInput: {
    flex: 1,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
    fontSize: fontSize.md,
    color: colors.text,
    borderWidth: 1,
    borderColor: colors.border,
  },
  searchSpinner: {
    marginLeft: spacing.sm,
  },
  listContent: {
    paddingBottom: spacing.lg,
  },
  emptyContainer: {
    flexGrow: 1,
  },

  // Search results
  searchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 2,
    backgroundColor: colors.surface,
  },
  searchRowInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  searchAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  searchAvatarText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.primaryDark,
  },
  searchName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  searchUsername: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  friendsBadge: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.medium,
    color: colors.success,
  },
  pendingBadge: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.medium,
    color: colors.warning,
  },
  noResults: {
    padding: spacing.xl,
    alignItems: 'center',
  },
  noResultsText: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
  },

  // Pending requests
  requestsSection: {
    paddingTop: spacing.sm,
  },
  sectionTitle: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.semibold,
    color: colors.text,
    paddingHorizontal: spacing.md,
    marginBottom: spacing.sm,
  },
  requestRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    backgroundColor: colors.surface,
  },
  requestAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.warningLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  requestAvatarText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.warning,
  },
  requestInfo: {
    flex: 1,
  },
  requestName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  requestUsername: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  requestActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  declineButton: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs + 2,
  },
  declineText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    fontWeight: fontWeight.medium,
  },
  separator: {
    height: 1,
    backgroundColor: colors.border,
    marginVertical: spacing.sm,
    marginHorizontal: spacing.md,
  },
});
