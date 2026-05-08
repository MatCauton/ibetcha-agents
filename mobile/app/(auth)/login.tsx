import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import Constants from 'expo-constants';
import { Input } from '../../src/components/Input';
import { Button } from '../../src/components/Button';
import { useAuthStore } from '../../src/stores/authStore';
import { colors } from '../../src/theme/colors';
import { spacing, fontSize, fontWeight } from '../../src/theme/spacing';
import { t } from '../../src/i18n';

// Lazy-require native-only modules so Metro doesn't try to bundle them on web
const GoogleSignin =
  Platform.OS !== 'web'
    ? (require('@react-native-google-signin/google-signin') as typeof import('@react-native-google-signin/google-signin')).GoogleSignin
    : null;

const AppleAuthentication =
  Platform.OS === 'ios'
    ? (require('expo-apple-authentication') as typeof import('expo-apple-authentication'))
    : null;

export default function LoginScreen() {
  const router = useRouter();
  const { login, loginWithGoogle, loginWithApple, isLoading, error, clearError } =
    useAuthStore();

  useEffect(() => {
    if (Platform.OS !== 'web') {
      GoogleSignin?.configure({
        webClientId: (Constants.expoConfig?.extra?.googleWebClientId as string | undefined) ?? '',
      });
    }
  }, []);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{
    email?: string;
    password?: string;
  }>({});

  function validate(): boolean {
    const errors: { email?: string; password?: string } = {};

    if (!email.trim()) {
      errors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      errors.email = 'Enter a valid email address';
    }

    if (!password) {
      errors.password = 'Password is required';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleLogin() {
    clearError();
    if (!validate()) return;

    try {
      await login({ email: email.trim(), password });
      // Router redirect handled by RootLayout
    } catch {
      // Error is set in the store
    }
  }

  async function handleGoogleSignIn() {
    if (!GoogleSignin) return;
    clearError();
    try {
      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
      const userInfo = await GoogleSignin.signIn();
      const idToken = userInfo.data?.idToken;
      if (!idToken) {
        Alert.alert(t('error'), 'Google sign-in did not return a token. Please try again.');
        return;
      }
      await loginWithGoogle(idToken);
      // Router redirect handled by RootLayout
    } catch (err: unknown) {
      // User cancelled — do not show an error banner
      if (isGoogleCancelError(err)) return;
      Alert.alert(t('error'), 'Google sign-in failed. Please try again.');
    }
  }

  async function handleAppleSignIn() {
    if (!AppleAuthentication) return;
    clearError();
    try {
      const credential = await AppleAuthentication.signInAsync({
        requestedScopes: [
          AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
          AppleAuthentication.AppleAuthenticationScope.EMAIL,
        ],
      });

      if (!credential.identityToken || !credential.authorizationCode) {
        Alert.alert(t('error'), 'Apple sign-in did not return required tokens. Please try again.');
        return;
      }

      await loginWithApple(credential.identityToken, credential.authorizationCode);
      // Router redirect handled by RootLayout
    } catch (err: unknown) {
      // User cancelled — do not show an error banner
      if (isAppleCancelError(err)) return;
      Alert.alert(t('error'), 'Apple sign-in failed. Please try again.');
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.header}>
            <Text style={styles.appName}>{t('appName')}</Text>
            <Text style={styles.subtitle}>Welcome back</Text>
          </View>

          {/* Dev quick-login — only visible in development builds */}
          {__DEV__ && (
            <View style={styles.devPanel}>
              <Text style={styles.devLabel}>DEV LOGIN</Text>
              <View style={styles.devButtons}>
                <TouchableOpacity
                  style={styles.devButton}
                  onPress={() => login({ email: 'alice@ibetcha.test', password: 'password' })}
                >
                  <Text style={styles.devButtonText}>Alice</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.devButton}
                  onPress={() => login({ email: 'bob@ibetcha.test', password: 'password' })}
                >
                  <Text style={styles.devButtonText}>Bob</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.devButton}
                  onPress={() => login({ email: 'charlie@ibetcha.test', password: 'password' })}
                >
                  <Text style={styles.devButtonText}>Charlie</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          {error && (
            <View style={styles.errorBanner}>
              <Text style={styles.errorBannerText}>{t('loginError')}</Text>
            </View>
          )}

          <View style={styles.form}>
            <Input
              label={t('email')}
              value={email}
              onChangeText={(text) => {
                setEmail(text);
                if (fieldErrors.email) {
                  setFieldErrors((prev) => ({ ...prev, email: undefined }));
                }
              }}
              placeholder="you@example.com"
              keyboardType="email-address"
              autoCapitalize="none"
              autoComplete="email"
              error={fieldErrors.email}
            />

            <Input
              label={t('password')}
              value={password}
              onChangeText={(text) => {
                setPassword(text);
                if (fieldErrors.password) {
                  setFieldErrors((prev) => ({ ...prev, password: undefined }));
                }
              }}
              placeholder="Your password"
              secureTextEntry
              autoComplete="password"
              error={fieldErrors.password}
            />

            <Button
              title={t('signIn')}
              onPress={handleLogin}
              loading={isLoading}
              fullWidth
              size="lg"
              style={styles.submitButton}
            />
          </View>

          {/* Social sign-in divider */}
          <View style={styles.dividerRow}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>{t('or')}</Text>
            <View style={styles.dividerLine} />
          </View>

          {/* Google Sign-In */}
          <TouchableOpacity
            style={styles.socialButton}
            onPress={handleGoogleSignIn}
            disabled={isLoading}
            activeOpacity={0.75}
          >
            <Text style={styles.socialButtonText}>Continue with Google</Text>
          </TouchableOpacity>

          {/* Apple Sign-In — iOS only */}
          {Platform.OS === 'ios' && AppleAuthentication && (
            <AppleAuthentication.AppleAuthenticationButton
              buttonType={AppleAuthentication.AppleAuthenticationButtonType.SIGN_IN}
              buttonStyle={AppleAuthentication.AppleAuthenticationButtonStyle.BLACK}
              cornerRadius={8}
              style={styles.appleButton}
              onPress={handleAppleSignIn}
            />
          )}

          <View style={styles.footer}>
            <Text style={styles.footerText}>{t('noAccount')} </Text>
            <TouchableOpacity onPress={() => router.push('/(auth)/register')}>
              <Text style={styles.footerLink}>{t('signUp')}</Text>
            </TouchableOpacity>
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function isGoogleCancelError(err: unknown): boolean {
  if (err === null || typeof err !== 'object') return false;
  // @react-native-google-signin/google-signin uses statusCodes
  const code = (err as Record<string, unknown>).code;
  return code === 'SIGN_IN_CANCELLED' || code === '12501';
}

function isAppleCancelError(err: unknown): boolean {
  if (err === null || typeof err !== 'object') return false;
  return (err as Record<string, unknown>).code === 'ERR_REQUEST_CANCELED';
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.background,
  },
  flex: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: spacing.lg,
    justifyContent: 'center',
  },
  header: {
    alignItems: 'center',
    marginBottom: spacing.xl,
  },
  appName: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.primary,
    marginBottom: spacing.xs,
  },
  subtitle: {
    fontSize: fontSize.lg,
    color: colors.textSecondary,
  },
  errorBanner: {
    backgroundColor: colors.dangerLight,
    padding: spacing.sm,
    borderRadius: 8,
    marginBottom: spacing.md,
  },
  errorBannerText: {
    color: colors.danger,
    fontSize: fontSize.sm,
    textAlign: 'center',
  },
  form: {
    marginBottom: spacing.lg,
  },
  submitButton: {
    marginTop: spacing.sm,
  },

  // Divider
  dividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: colors.border,
  },
  dividerText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginHorizontal: spacing.sm,
  },

  // Social buttons
  socialButton: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingVertical: spacing.sm + 2,
    alignItems: 'center',
    backgroundColor: colors.surface,
    marginBottom: spacing.sm,
  },
  socialButtonText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.medium,
    color: colors.text,
  },
  appleButton: {
    height: 44,
    marginBottom: spacing.sm,
  },

  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: spacing.lg,
  },
  footerText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  footerLink: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: colors.primary,
  },

  // Dev panel
  devPanel: {
    marginBottom: spacing.md,
    padding: spacing.sm,
    borderRadius: 8,
    backgroundColor: '#FFF9C4',
    borderWidth: 1,
    borderColor: '#F59E0B',
    alignItems: 'center',
    gap: spacing.xs,
  },
  devLabel: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
    color: '#92400E',
    letterSpacing: 1,
  },
  devButtons: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  devButton: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: 6,
    backgroundColor: '#F59E0B',
  },
  devButtonText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
    color: '#FFFFFF',
  },
});
