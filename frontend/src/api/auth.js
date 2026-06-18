import { apiFetch } from './client';
import { clearToken, setToken } from './token';

/** Create an account. Stores the returned token and returns the user. */
export async function register(payload) {
  const data = await apiFetch('/api/auth/register', { method: 'POST', body: payload });
  setToken(data.token);
  return data.user;
}

/** Log in. Stores the returned token and returns the user. */
export async function login(email, password) {
  const data = await apiFetch('/api/auth/login', { method: 'POST', body: { email, password } });
  setToken(data.token);
  return data.user;
}

/** Resolve the current user from the stored token (used to restore a session on load). */
export function fetchMe() {
  return apiFetch('/api/auth/me');
}

/** Stateless logout: drop the token. */
export function logout() {
  clearToken();
}
