import { useCallback, useEffect, useState } from 'react';
import { getOverview, getQuote, getSeries } from '../api/marketData';
import { LIVE_APIS } from '../config';

/**
 * Loads everything the dashboard needs for one symbol:
 *  - quote (Finnhub, required)
 *  - the full 30-min series (Yahoo via backend cache) — resampled per timeframe on the client
 *  - company overview (Alpha Vantage)
 * Series and overview are best-effort so one source failing never blanks the page.
 */
export function useMarketData(symbol) {
  const [quote, setQuote] = useState(null);
  const [series, setSeries] = useState(null);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    if (!symbol) return;
    setLoading(true);
    setError(null);
    try {
      const [quoteRes, seriesRes, overviewRes] = await Promise.all([
        LIVE_APIS ? getQuote(symbol) : Promise.resolve(null),
        getSeries(symbol).catch((err) => {
          console.warn('series unavailable:', err.message);
          return null;
        }),
        LIVE_APIS
          ? getOverview(symbol).catch((err) => {
              console.warn('overview unavailable:', err.message);
              return null;
            })
          : Promise.resolve(null),
      ]);
      setQuote(quoteRes);
      setSeries(seriesRes ? { coarse: seriesRes.coarse ?? [], fine: seriesRes.fine ?? [] } : null);
      setOverview(overviewRes);
    } catch (err) {
      setError(err.message || 'Failed to load market data');
    } finally {
      setLoading(false);
    }
  }, [symbol]);

  useEffect(() => {
    // Fetch-on-change: load() intentionally sets loading/error state up front.
    load(); // eslint-disable-line react-hooks/set-state-in-effect
  }, [load]);

  return { quote, series, overview, loading, error, reload: load };
}
