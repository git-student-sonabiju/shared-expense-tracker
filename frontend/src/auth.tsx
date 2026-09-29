import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, setUnauthorizedHandler, tokenStore } from './api';
import type { Session, User } from './types';

interface AuthState {
  /** undefined while the stored token is being checked on startup. */
  user: User | null | undefined;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, displayName: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Set when the user was signed out because their session expired. */
  notice: string | null;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null | undefined>(tokenStore.get() ? undefined : null);
  const [notice, setNotice] = useState<string | null>(null);

  const startSession = useCallback((session: Session) => {
    tokenStore.set(session.token);
    setNotice(null);
    setUser(session.user);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      tokenStore.set(null);
      setNotice('Your session has ended. Please log in again.');
      setUser(null);
    });
    if (tokenStore.get()) {
      api
        .me()
        .then(setUser)
        .catch(() => {
          tokenStore.set(null);
          setUser(null);
        });
    }
  }, []);

  const value: AuthState = {
    user,
    notice,
    login: async (username, password) => startSession(await api.login(username, password)),
    register: async (username, displayName, password) =>
      startSession(await api.register(username, displayName, password)),
    logout: async () => {
      try {
        await api.logout();
      } catch {
        /* the session is discarded locally either way */
      } finally {
        // Even if the server is unreachable, forget the token locally.
        tokenStore.set(null);
        setUser(null);
      }
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
