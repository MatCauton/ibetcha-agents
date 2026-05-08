import React, { useCallback, useEffect } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Text,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useBetStore } from '../../src/stores/betStore';
import { BetCard } from '../../src/components/BetCard';
import { EmptyState } from '../../src/components/EmptyState';
import { colors } from '../../src/theme/colors';
import { spacing, borderRadius, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';
import type { Bet } from '../../src/types/domain';

export default function HomeScreen() {
  const router = useRouter();
  const { bets, isLoading, isRefreshing, error, fetchBets, fetchMoreBets } =
    useBetStore();

  useEffect(() => {
    fetchBets();
  }, []);

  const handleRefresh = useCallback(() => {
    fetchBets(true);
  }, [fetchBets]);

  const handleEndReached = useCallback(() => {
    fetchMoreBets();
  }, [fetchMoreBets]);

  const renderItem = useCallback(
    ({ item }: { item: Bet }) => <BetCard bet={item} />,
    [],
  );

  const keyExtractor = useCallback((item: Bet) => item.betId, []);

  if (isLoading && bets.length === 0) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error && bets.length === 0) {
    return (
      <View style={styles.centered}>
        <EmptyState
          title={t('error')}
          description={error}
          actionLabel={t('retry')}
          onAction={() => fetchBets()}
        />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={bets}
        renderItem={renderItem}
        keyExtractor={keyExtractor}
        contentContainerStyle={
          bets.length === 0 ? styles.emptyContainer : styles.listContent
        }
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={handleRefresh}
            tintColor={colors.primary}
          />
        }
        onEndReached={handleEndReached}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={
          <EmptyState
            title={t('noBets')}
            description={t('noBetsDescription')}
            actionLabel={t('createBet')}
            onAction={() => router.push('/bet/create')}
          />
        }
      />

      {/* FAB */}
      <TouchableOpacity
        style={styles.fab}
        activeOpacity={0.8}
        onPress={() => router.push('/bet/create')}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>
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
  listContent: {
    paddingTop: spacing.sm,
    paddingBottom: spacing.xxl + spacing.xl,
  },
  emptyContainer: {
    flexGrow: 1,
  },
  fab: {
    position: 'absolute',
    bottom: spacing.lg,
    right: spacing.lg,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.fab,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 6,
    shadowColor: colors.primary,
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.3,
    shadowRadius: 6,
  },
  fabText: {
    fontSize: 28,
    fontWeight: fontWeight.bold,
    color: colors.textInverse,
    marginTop: -2,
  },
});
