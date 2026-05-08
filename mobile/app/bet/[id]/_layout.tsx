import React from 'react';
import { Stack } from 'expo-router';
import { colors } from '../../../src/theme/colors';
import { fontSize, fontWeight } from '../../../src/theme/spacing';

export default function BetSubLayout() {
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
      <Stack.Screen name="index" options={{ title: 'Bet Detail' }} />
      <Stack.Screen name="win-card" options={{ title: 'Win Card' }} />
    </Stack>
  );
}
