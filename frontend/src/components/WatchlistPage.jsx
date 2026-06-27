import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertTriangle, Bookmark, Check, Loader2, LogIn, Pencil, Plus, RefreshCw, Trash2, TrendingUp, X,
} from 'lucide-react';
import {
  addSymbol, createWatchlist, deleteWatchlist, getWatchlists, removeSymbol, renameWatchlist,
} from '../api/watchlists';
import { displaySymbol, formatPercent } from '../utils/format';
import { useAuth } from '../auth/useAuth';
import { useCurrency } from '../currency/useCurrency';
import T from './T';

/** The current user's watchlists: pick a list on the left, manage its symbols on the right. */
export default function WatchlistPage() {
  const { user } = useAuth();
  const { requireLogin } = useOutletContext();
  const navigate = useNavigate();

  const [lists, setLists] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getWatchlists();
      const arr = Array.isArray(data) ? data : [];
      setLists(arr);
      setSelectedId((prev) => (arr.some((l) => l.id === prev) ? prev : arr[0]?.id ?? null));
    } catch (err) {
      setError(err.message || 'Could not load your watchlists');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user) reload(); // eslint-disable-line react-hooks/set-state-in-effect
  }, [user, reload]);

  if (!user) {
    return (
      <main className="max-w-[1200px] mx-auto px-4 sm:px-8 py-24 flex flex-col items-center gap-5 text-center">
        <div className="w-14 h-14 rounded-2xl bg-surface-container-high flex items-center justify-center">
          <Bookmark className="w-7 h-7 text-on-surface-variant" />
        </div>
        <p className="font-headline font-bold text-lg"><T>Log in to see your watchlists</T></p>
        <button
          type="button"
          onClick={() => requireLogin('/watchlist')}
          className="flex items-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-[0.2em] hover:brightness-110 active:scale-95 transition-all"
        >
          <LogIn className="w-4 h-4" />
          <T>Log in</T>
        </button>
      </main>
    );
  }

  const selected = lists.find((l) => l.id === selectedId) ?? null;

  return (
    <main className="max-w-[1200px] mx-auto px-4 sm:px-8 py-10 mb-24 lg:mb-10">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <Bookmark className="text-primary w-6 h-6" />
          <h2 className="font-headline text-3xl sm:text-4xl font-extrabold tracking-tight"><T>Watchlists</T></h2>
        </div>
        <button
          type="button"
          onClick={reload}
          title="Refresh"
          className="flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors text-xs font-bold uppercase tracking-wider"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <T>Refresh</T>
        </button>
      </div>

      {loading && lists.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-4 py-28 text-on-surface-variant">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
          <p className="text-sm uppercase tracking-widest font-bold"><T>Loading watchlists…</T></p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center gap-5 py-24 text-center">
          <div className="w-14 h-14 rounded-2xl bg-error/10 flex items-center justify-center">
            <AlertTriangle className="w-7 h-7 text-error" />
          </div>
          <p className="text-sm text-on-surface-variant max-w-md">{error}</p>
          <button
            type="button"
            onClick={reload}
            className="flex items-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-[0.2em] hover:brightness-110 active:scale-95 transition-all"
          >
            <RefreshCw className="w-4 h-4" />
            <T>Retry</T>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-8">
          <ListSidebar lists={lists} selectedId={selectedId} onSelect={setSelectedId} onChanged={reload} />
          {selected && (
            <ListPanel
              key={selected.id}
              list={selected}
              onChanged={reload}
              onBrowse={() => navigate('/app')}
              onOpen={(symbol) => navigate(`/app/${encodeURIComponent(symbol)}`)}
            />
          )}
        </div>
      )}
    </main>
  );
}

function ListSidebar({ lists, selectedId, onSelect, onChanged }) {
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function submit(event) {
    event.preventDefault();
    const value = name.trim();
    if (!value) return;
    setBusy(true);
    setErr(null);
    try {
      await createWatchlist(value);
      setName('');
      setCreating(false);
      await onChanged();
    } catch (e) {
      setErr(e.message || 'Could not create list');
    } finally {
      setBusy(false);
    }
  }

  return (
    <aside className="flex flex-col gap-3">
      <div className="flex flex-col gap-2">
        {lists.map((list) => {
          const active = list.id === selectedId;
          return (
            <button
              key={list.id}
              type="button"
              onClick={() => onSelect(list.id)}
              className={`flex items-center justify-between gap-2 px-4 py-3 rounded-xl border text-left transition-colors ${
                active
                  ? 'bg-primary/10 border-primary/40 text-primary'
                  : 'bg-surface-container-low border-outline-variant/10 text-on-surface hover:border-primary/30'
              }`}
            >
              <span className="font-headline font-bold truncate">{list.name}</span>
              <span className="text-[11px] font-bold text-on-surface-variant shrink-0">{list.items?.length ?? 0}</span>
            </button>
          );
        })}
      </div>

      {creating ? (
        <form onSubmit={submit} className="flex items-center gap-2">
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={64}
            placeholder="List name"
            className="flex-1 min-w-0 bg-surface-container-lowest rounded-lg px-3 py-2 text-sm border border-outline/40 focus:border-primary outline-none transition-colors"
          />
          <IconButton type="submit" title="Create" busy={busy} icon={<Check className="w-4 h-4" />} />
          <IconButton title="Cancel" onClick={() => { setCreating(false); setName(''); setErr(null); }} icon={<X className="w-4 h-4" />} />
        </form>
      ) : (
        <button
          type="button"
          onClick={() => setCreating(true)}
          className="flex items-center justify-center gap-2 px-4 py-3 rounded-xl border border-dashed border-outline-variant/40 text-on-surface-variant hover:text-primary hover:border-primary/40 transition-colors text-xs font-bold uppercase tracking-wider"
        >
          <Plus className="w-4 h-4" />
          <T>New list</T>
        </button>
      )}
      {err && <p className="text-error text-xs">{err}</p>}
    </aside>
  );
}

function ListPanel({ list, onChanged, onBrowse, onOpen }) {
  const [renaming, setRenaming] = useState(false);
  const [name, setName] = useState(list.name);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [symbol, setSymbol] = useState('');
  const [busy, setBusy] = useState(null); // 'rename' | 'delete' | 'add' | `remove-<id>`
  const [err, setErr] = useState(null);

  const items = list.items ?? [];

  async function doRename(event) {
    event.preventDefault();
    const value = name.trim();
    if (!value || value === list.name) { setRenaming(false); setName(list.name); return; }
    setBusy('rename');
    setErr(null);
    try {
      await renameWatchlist(list.id, value);
      setRenaming(false);
      await onChanged();
    } catch (e) {
      setErr(e.message || 'Could not rename list');
      setBusy(null);
    }
  }

  async function doDelete() {
    setBusy('delete');
    setErr(null);
    try {
      await deleteWatchlist(list.id);
      await onChanged();
    } catch (e) {
      setErr(e.message || 'Could not delete list');
      setBusy(null);
    }
  }

  async function doAdd(event) {
    event.preventDefault();
    const value = symbol.trim().toUpperCase();
    if (!value) return;
    setBusy('add');
    setErr(null);
    try {
      await addSymbol(list.id, value);
      setSymbol('');
      await onChanged();
    } catch (e) {
      setErr(e.message || 'Could not add symbol');
    } finally {
      setBusy(null);
    }
  }

  async function doRemove(itemId) {
    setBusy(`remove-${itemId}`);
    setErr(null);
    try {
      await removeSymbol(list.id, itemId);
      await onChanged();
    } catch (e) {
      setErr(e.message || 'Could not remove symbol');
      setBusy(null);
    }
  }

  return (
    <section className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4 border-b border-outline-variant/20 pb-4">
        {renaming ? (
          <form onSubmit={doRename} className="flex items-center gap-2 flex-1">
            <input
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={64}
              className="flex-1 min-w-0 bg-surface-container-lowest rounded-lg px-3 py-2 text-lg font-headline font-bold border border-outline/40 focus:border-primary outline-none"
            />
            <IconButton type="submit" title="Save" busy={busy === 'rename'} icon={<Check className="w-4 h-4" />} />
            <IconButton title="Cancel" onClick={() => { setRenaming(false); setName(list.name); }} icon={<X className="w-4 h-4" />} />
          </form>
        ) : (
          <>
            <h3 className="font-headline text-2xl font-extrabold tracking-tight truncate">{list.name}</h3>
            <div className="flex items-center gap-1 shrink-0">
              <IconButton title="Rename list" onClick={() => setRenaming(true)} icon={<Pencil className="w-4 h-4" />} />
              {confirmDelete ? (
                <div className="flex items-center gap-1">
                  <span className="text-xs text-on-surface-variant"><T>Delete?</T></span>
                  <IconButton title="Confirm delete" danger busy={busy === 'delete'} onClick={doDelete} icon={<Check className="w-4 h-4" />} />
                  <IconButton title="Cancel" onClick={() => setConfirmDelete(false)} icon={<X className="w-4 h-4" />} />
                </div>
              ) : (
                <IconButton title="Delete list" danger onClick={() => setConfirmDelete(true)} icon={<Trash2 className="w-4 h-4" />} />
              )}
            </div>
          </>
        )}
      </div>

      <form onSubmit={doAdd} className="flex items-center gap-2">
        <input
          value={symbol}
          onChange={(e) => setSymbol(e.target.value)}
          maxLength={32}
          placeholder="Add symbol (e.g. AAPL)"
          className="flex-1 bg-surface-container-lowest rounded-lg px-4 py-3 text-sm border border-outline/40 focus:border-primary outline-none transition-colors uppercase placeholder:normal-case"
        />
        <button
          type="submit"
          disabled={busy === 'add' || !symbol.trim()}
          className="flex items-center gap-2 px-5 py-3 rounded-lg bg-primary text-on-primary font-bold text-xs uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all disabled:opacity-50"
        >
          {busy === 'add' ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
          <T>Add</T>
        </button>
      </form>

      {err && <p className="text-error text-sm">{err}</p>}

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-5 py-16 text-center">
          <div className="w-14 h-14 rounded-2xl bg-surface-container-high flex items-center justify-center">
            <Bookmark className="w-7 h-7 text-on-surface-variant" />
          </div>
          <p className="text-sm text-on-surface-variant max-w-sm">
            <T>No symbols yet. Add one above, or find a stock in the Market view.</T>
          </p>
          <button
            type="button"
            onClick={onBrowse}
            className="flex items-center gap-2 px-5 py-3 rounded-xl bg-surface-container-high text-on-surface font-bold text-xs uppercase tracking-[0.2em] border border-outline-variant/30 hover:text-primary transition-colors"
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
              removing={busy === `remove-${item.id}`}
              onRemove={() => doRemove(item.id)}
              onOpen={() => onOpen(item.symbol)}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function WatchlistRow({ item, removing, onRemove, onOpen }) {
  const { formatMoney } = useCurrency();
  const pct = item.changePct;
  const trendColor = pct == null ? 'text-on-surface-variant' : pct >= 0 ? 'text-primary' : 'text-error';

  function handleKeyDown(event) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onOpen();
    }
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={handleKeyDown}
      title={`View ${item.symbol}`}
      className="group flex flex-col gap-4 sm:grid sm:grid-cols-[1.5fr_1fr_1fr_1fr_auto] sm:gap-4 sm:items-center bg-surface-container-low rounded-xl px-6 py-5 border border-outline-variant/10 cursor-pointer hover:border-primary/30 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 transition-colors"
    >
      <div className="flex items-center justify-between">
        <div className="flex flex-col">
          <span className="font-headline font-bold text-lg group-hover:text-primary transition-colors">{displaySymbol(item.symbol)}</span>
          <span className="text-[11px] text-on-surface-variant"><T>Added</T> {formatDate(item.addedAt)}</span>
        </div>
        <RemoveButton className="sm:hidden" removing={removing} onRemove={onRemove} />
      </div>

      <Cell label="Starting" value={formatMoney(item.startPrice, item.currency, item.type)} />
      <Cell label="Current" value={formatMoney(item.currentPrice, item.currency, item.type)} />

      <div className="flex justify-between sm:flex-col sm:items-end">
        <span className="sm:hidden text-[10px] uppercase font-bold tracking-wider text-on-surface-variant"><T>Performance</T></span>
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
      <span className="sm:hidden text-[10px] uppercase font-bold tracking-wider text-on-surface-variant"><T>{label}</T></span>
      <span className="font-headline font-semibold">{value}</span>
    </div>
  );
}

function RemoveButton({ removing, onRemove, className = '' }) {
  return (
    <button
      type="button"
      onClick={(event) => { event.stopPropagation(); onRemove(); }}
      disabled={removing}
      title="Remove from list"
      className={`p-2 rounded-lg text-on-surface-variant hover:text-error hover:bg-error/10 transition-colors disabled:opacity-50 ${className}`}
    >
      {removing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
    </button>
  );
}

function IconButton({ icon, onClick, title, busy = false, danger = false, type = 'button' }) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={busy}
      title={title}
      className={`p-2 rounded-lg transition-colors disabled:opacity-50 ${
        danger ? 'text-on-surface-variant hover:text-error hover:bg-error/10' : 'text-on-surface-variant hover:text-primary hover:bg-primary/10'
      }`}
    >
      {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : icon}
    </button>
  );
}

function formatDate(iso) {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}
