import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, Bookmark, Loader2, RefreshCw, Trash2, TrendingUp } from 'lucide-react';
import { getWatchlist, removeFromWatchlist } from '../api/watchlist';
import { formatPercent, formatPrice } from '../utils/format';
import T from './T';

/** The current user's watchlist: each saved stock with its starting price, current price and performance. */
export default function Watchlist({ onBrowse }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [removingId, setRemovingId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getWatchlist();
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Could not load your watchlist');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(); // eslint-disable-line react-hooks/set-state-in-effect
  }, [load]);

  async function handleRemove(id) {
    setRemovingId(id);
    try {
      await removeFromWatchlist(id);
      setItems((prev) => prev.filter((item) => item.id !== id));
    } catch (err) {
      setError(err.message || 'Could not remove item');
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <main className="max-w-[1100px] mx-auto px-4 sm:px-8 py-10 mb-24 lg:mb-10">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <Bookmark className="text-primary w-6 h-6" />
          <h2 className="font-headline text-3xl sm:text-4xl font-extrabold tracking-tight">
            <T>Watchlist</T>
          </h2>
        </div>
        <button
          type="button"
          onClick={load}
          title="Refresh"
          className="flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors text-xs font-bold uppercase tracking-wider"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <T>Refresh</T>
        </button>
      </div>

      {loading && items.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-4 py-28 text-on-surface-variant">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
          <p className="text-sm uppercase tracking-widest font-bold"><T>Loading watchlist…</T></p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center gap-5 py-24 text-center">
          <div className="w-14 h-14 rounded-2xl bg-error/10 flex items-center justify-center">
            <AlertTriangle className="w-7 h-7 text-error" />
          </div>
          <p className="text-sm text-on-surface-variant max-w-md">{error}</p>
          <button
            type="button"
            onClick={load}
            className="flex items-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-[0.2em] hover:brightness-110 active:scale-95 transition-all"
          >
            <RefreshCw className="w-4 h-4" />
            <T>Retry</T>
          </button>
        </div>
      ) : items.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-5 py-24 text-center">
          <div className="w-14 h-14 rounded-2xl bg-surface-container-high flex items-center justify-center">
            <Bookmark className="w-7 h-7 text-on-surface-variant" />
          </div>
          <div className="flex flex-col gap-1">
            <p className="font-headline font-bold text-lg"><T>Your watchlist is empty</T></p>
            <p className="text-sm text-on-surface-variant max-w-sm">
              <T>Find a stock in the Market view and tap "Watch" to track its performance from today.</T>
            </p>
          </div>
          <button
            type="button"
            onClick={onBrowse}
            className="flex items-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-[0.2em] hover:brightness-110 active:scale-95 transition-all"
          >
            <TrendingUp className="w-4 h-4" />
            <T>Browse market</T>
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <div className="hidden sm:grid grid-cols-[1.5fr_1fr_1fr_1fr_auto] gap-4 px-6 text-[10px] uppercase font-bold tracking-[0.15em] text-on-surface-variant">
            <span><T>Symbol</T></span>
            <span className="text-right"><T>Starting</T></span>
            <span className="text-right"><T>Current</T></span>
            <span className="text-right"><T>Performance</T></span>
            <span />
          </div>
          {items.map((item) => (
            <WatchlistRow
              key={item.id}
              item={item}
              removing={removingId === item.id}
              onRemove={() => handleRemove(item.id)}
            />
          ))}
        </div>
      )}
    </main>
  );
}

function WatchlistRow({ item, removing, onRemove }) {
  const pct = item.changePct;
  const trendColor = pct == null ? 'text-on-surface-variant' : pct >= 0 ? 'text-primary' : 'text-error';

  return (
    <div className="flex flex-col gap-4 sm:grid sm:grid-cols-[1.5fr_1fr_1fr_1fr_auto] sm:gap-4 sm:items-center bg-surface-container-low rounded-xl px-6 py-5 border border-outline-variant/10 hover:border-primary/30 transition-colors">
      <div className="flex items-center justify-between">
        <div className="flex flex-col">
          <span className="font-headline font-bold text-lg">{item.symbol}</span>
          <span className="text-[11px] text-on-surface-variant"><T>Added</T> {formatDate(item.addedAt)}</span>
        </div>
        <RemoveButton className="sm:hidden" removing={removing} onRemove={onRemove} />
      </div>

      <Cell label="Starting" value={formatPrice(item.startPrice)} />
      <Cell label="Current" value={formatPrice(item.currentPrice)} />

      <div className="flex justify-between sm:flex-col sm:items-end">
        <span className="sm:hidden text-[10px] uppercase font-bold tracking-wider text-on-surface-variant">
          <T>Performance</T>
        </span>
        <span className={`font-headline font-bold ${trendColor}`}>{formatPercent(pct)}</span>
      </div>

      <div className="hidden sm:flex justify-end">
        <RemoveButton removing={removing} onRemove={onRemove} />
      </div>
    </div>
  );
}

function Cell({ label, value }) {
  return (
    <div className="flex justify-between sm:block sm:text-right">
      <span className="sm:hidden text-[10px] uppercase font-bold tracking-wider text-on-surface-variant">
        <T>{label}</T>
      </span>
      <span className="font-headline font-semibold">{value}</span>
    </div>
  );
}

function RemoveButton({ removing, onRemove, className = '' }) {
  return (
    <button
      type="button"
      onClick={onRemove}
      disabled={removing}
      title="Remove from watchlist"
      className={`p-2 rounded-lg text-on-surface-variant hover:text-error hover:bg-error/10 transition-colors disabled:opacity-50 ${className}`}
    >
      {removing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
    </button>
  );
}

function formatDate(iso) {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}
