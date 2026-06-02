import { useEffect, useRef, useState } from 'react';
import { AlertTriangle, Loader2, Search } from 'lucide-react';
import { searchSymbols } from '../api/marketData';
import { LIVE_APIS } from '../config';

function friendlyError(err) {
  const msg = err?.message ?? '';
  if (msg.includes('Failed to fetch') || /\b50[234]\b/.test(msg)) {
    return 'Backend not reachable — is it running on :8080?';
  }
  return msg || 'Search failed';
}

export default function SymbolSearch({ onSelect }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searched, setSearched] = useState(false);
  const boxRef = useRef(null);

  useEffect(() => {
    if (!LIVE_APIS) {
      // API-saving mode: no autocomplete (Alpha Vantage). Enter-to-submit still works.
      return undefined;
    }
    const q = query.trim();
    const timer = setTimeout(async () => {
      if (q.length < 1) {
        setResults([]);
        setError(null);
        setSearched(false);
        setLoading(false);
        return;
      }
      setLoading(true);
      setError(null);
      try {
        const matches = await searchSymbols(q);
        setResults(Array.isArray(matches) ? matches.slice(0, 8) : []);
      } catch (err) {
        setResults([]);
        setError(friendlyError(err));
      } finally {
        setSearched(true);
        setLoading(false);
        setOpen(true);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    function handleOutside(event) {
      if (boxRef.current && !boxRef.current.contains(event.target)) setOpen(false);
    }
    document.addEventListener('mousedown', handleOutside);
    return () => document.removeEventListener('mousedown', handleOutside);
  }, []);

  function choose(symbol) {
    onSelect(symbol);
    setQuery('');
    setResults([]);
    setError(null);
    setSearched(false);
    setOpen(false);
  }

  function handleSubmit(event) {
    event.preventDefault();
    const value = query.trim().toUpperCase();
    if (value) choose(value);
  }

  const showDropdown = open && (results.length > 0 || error || (searched && !loading));

  return (
    <div ref={boxRef} className="relative">
      <form
        onSubmit={handleSubmit}
        className="flex items-center gap-2 bg-surface-container-lowest rounded-lg px-3 py-2 border border-outline/40 focus-within:border-primary transition-colors"
      >
        {loading ? (
          <Loader2 className="w-4 h-4 text-on-surface-variant animate-spin" />
        ) : (
          <Search className="w-4 h-4 text-on-surface-variant" />
        )}
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onFocus={() => (results.length > 0 || error) && setOpen(true)}
          placeholder={LIVE_APIS ? 'Search ticker…' : 'Ticker + Enter…'}
          className="bg-transparent outline-none text-sm text-on-surface placeholder:text-on-surface-variant w-28 sm:w-40"
        />
      </form>

      {showDropdown && (
        <div className="absolute right-0 mt-2 w-72 max-h-80 overflow-auto bg-surface-container-high/95 backdrop-blur-md rounded-xl shadow-2xl border border-outline-variant/20 z-50 py-2">
          {error ? (
            <div className="flex items-start gap-2 px-4 py-3 text-error">
              <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
              <span className="text-xs leading-snug">{error}</span>
            </div>
          ) : results.length > 0 ? (
            <ul>
              {results.map((result) => (
                <li key={`${result.symbol}-${result.region}`}>
                  <button
                    type="button"
                    onClick={() => choose(result.symbol)}
                    className="w-full text-left px-4 py-2 hover:bg-primary/10 transition-colors"
                  >
                    <span className="font-headline font-bold text-sm text-on-surface">{result.symbol}</span>
                    <span className="block text-[11px] text-on-surface-variant truncate">
                      {result.name}
                      {result.region ? ` · ${result.region}` : ''}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <div className="px-4 py-3 text-xs text-on-surface-variant">No matches found.</div>
          )}
        </div>
      )}
    </div>
  );
}
