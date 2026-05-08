export const colors = {
  // Primary palette
  primary: '#2563EB',
  primaryDark: '#1D4ED8',
  primaryLight: '#93C5FD',

  // Accent / positive
  success: '#16A34A',
  successLight: '#BBF7D0',

  // Warning
  warning: '#F59E0B',
  warningLight: '#FDE68A',

  // Destructive / negative
  danger: '#DC2626',
  dangerLight: '#FECACA',

  // Background
  background: '#F8FAFC',
  surface: '#FFFFFF',
  surfaceSecondary: '#F1F5F9',

  // Text
  text: '#0F172A',
  textSecondary: '#64748B',
  textTertiary: '#94A3B8',
  textInverse: '#FFFFFF',

  // Borders
  border: '#E2E8F0',
  borderFocus: '#2563EB',

  // Status-specific
  statusPending: '#F59E0B',
  statusActive: '#2563EB',
  statusResolved: '#16A34A',
  statusDisputed: '#DC2626',
  statusExpired: '#94A3B8',
  statusCancelled: '#94A3B8',

  // Misc
  overlay: 'rgba(0, 0, 0, 0.5)',
  shadow: 'rgba(0, 0, 0, 0.1)',
  fab: '#2563EB',
} as const;

export type ColorKey = keyof typeof colors;
