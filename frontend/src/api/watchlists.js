import { apiFetch } from './client';

/** All of the current user's watchlists, each with its items (already price-enriched). */
export function getWatchlists() {
  return apiFetch('/api/watchlists');
}

export function createWatchlist(name) {
  return apiFetch('/api/watchlists', { method: 'POST', body: { name } });
}

export function renameWatchlist(id, name) {
  return apiFetch(`/api/watchlists/${id}`, { method: 'PUT', body: { name } });
}

export function deleteWatchlist(id) {
  return apiFetch(`/api/watchlists/${id}`, { method: 'DELETE' });
}

/** Add a symbol to a list; the backend captures the current price as the starting price. */
export function addSymbol(listId, symbol) {
  return apiFetch(`/api/watchlists/${listId}/items`, { method: 'POST', body: { symbol } });
}

export function removeSymbol(listId, itemId) {
  return apiFetch(`/api/watchlists/${listId}/items/${itemId}`, { method: 'DELETE' });
}
