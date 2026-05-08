import { useEffect } from 'react';
import { useAuthStore } from '../stores/authStore';

export function useAuth() {
  const { initialize, isInitialized, isAuthenticated, user, isLoading, error } =
    useAuthStore();

  useEffect(() => {
    if (!isInitialized) {
      initialize();
    }
  }, [isInitialized, initialize]);

  return {
    isInitialized,
    isAuthenticated,
    user,
    isLoading,
    error,
  };
}
