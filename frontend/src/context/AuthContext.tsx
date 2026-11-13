import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { TOKEN_KEY, readToken, setUnauthorizedHandler } from '../api/client';
import { authApi, userApi } from '../api/endpoints';
import type { AuthResponse, User } from '../types';

interface AuthState {
  user: User | null;
  initializing: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (body: { fullName: string; email: string; password: string; phone?: string }) => Promise<User>;
  logout: () => void;
  setUser: (user: User) => void;
}

const AuthContext = createContext<AuthState | null>(null);

function storeToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage unavailable (private mode): the session lasts until reload
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);

  const logout = useCallback(() => {
    storeToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    if (!readToken()) {
      setInitializing(false);
      return;
    }
    userApi.me()
      .then(setUser)
      .catch(() => storeToken(null))
      .finally(() => setInitializing(false));
  }, [logout]);

  const accept = useCallback((response: AuthResponse) => {
    storeToken(response.token);
    setUser(response.user);
    return response.user;
  }, []);

  const value = useMemo<AuthState>(() => ({
    user,
    initializing,
    login: (email, password) => authApi.login(email, password).then(accept),
    register: (body) => authApi.register(body).then(accept),
    logout,
    setUser,
  }), [user, initializing, accept, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
