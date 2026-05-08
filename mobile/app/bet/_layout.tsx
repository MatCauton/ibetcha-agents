import React from 'react';
import { Stack } from 'expo-router';
import { colors } from '../../src/theme/colors';
import { fontSize, fontWeight } from '../../src/theme/spacing';

export default function BetLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.background },
        headerTitleStyle: {
          fontSize: fontSize.lg,
          fontWeight: fontWeight.semibold,
          color: colors.text,
        },
        headerTintColor: colors.primary,
        headerShadowVisible: false,
      }}
    >
      <Stack.Screen
        name="create"
        options={{ title: 'Quick Bet', presentation: 'modal' }}
      />
      <Stack.Screen name="[id]" options={{ title: 'Bet Detail' }} />
    </Stack>
  );
}
