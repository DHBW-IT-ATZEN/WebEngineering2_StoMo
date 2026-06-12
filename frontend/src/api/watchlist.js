import { apiFetch } from './client';

/** Current user's watchlist with performance since each stock was saved. */
export function getWatchlist() {
  return apiFetch('/api/watchlist');
}

/** Add a symbol; the backend captures the current price as the starting price. */
export function addToWatchlist(symbol) {
  return apiFetch('/api/watchlist', { method: 'POST', body: { symbol } });
}

export function removeFromWatchlist(id) {
  return apiFetch(`/api/watchlist/${id}`, { method: 'DELETE' });
}
