import { useCallback, useEffect, useState } from 'react';
import { AuthContext } from './authContext';
import {
  fetchMe,
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
} from '../api/auth';
import { getToken } from '../api/token';

/**
 * Holds the authenticated user. On mount it restores the session from a stored token
 * (calling /api/auth/me); an invalid/expired token is discarded. Mirrors the ThemeProvider.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // ready=true immediately when there's no token; otherwise it flips once /me resolves.
  const [ready, setReady] = useState(() => !getToken());

  useEffect(() => {
    if (!getToken()) return undefined;
    let active = true;
    fetchMe()
      .then((restored) => { if (active) setUser(restored); })
      .catch(() => apiLogout()) // token invalid/expired
      .finally(() => { if (active) setReady(true); });
    return () => { active = false; };
  }, []);

  const login = useCallback(async (email, password) => {
    const loggedIn = await apiLogin(email, password);
    setUser(loggedIn);
    return loggedIn;
  }, []);

  const register = useCallback(async (payload) => {
    const created = await apiRegister(payload);
    setUser(created);
    return created;
  }, []);

  const logout = useCallback(() => {
    apiLogout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, ready, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
