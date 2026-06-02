import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { YodaTextContext } from './yodaTextContext';
import { useTheme } from './useTheme';
import { translateBatch } from '../api/yoda';

/**
 * Collects the UI strings that <T> wants translated, debounces them into a single
 * batch request, and caches the results in memory. The backend caches persistently,
 * so each phrase costs at most one paid API call across the whole app's lifetime.
 */
export function YodaTextProvider({ children }) {
  const { isYoda } = useTheme();
  const [cache, setCache] = useState({});
  const cacheRef = useRef(cache);
  const pending = useRef(new Set());
  const timer = useRef(null);

  useEffect(() => {
    cacheRef.current = cache;
  }, [cache]);

  const flush = useCallback(() => {
    const texts = [...pending.current].filter((s) => !(s in cacheRef.current));
    pending.current.clear();
    if (texts.length === 0) return;
    translateBatch(texts)
      .then((map) => setCache((prev) => ({ ...prev, ...map })))
      .catch(() => {
        /* translation is best-effort; keep showing the source text */
      });
  }, []);

  const request = useCallback(
    (text) => {
      if (!text || text in cacheRef.current) return;
      pending.current.add(text);
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(flush, 80);
    },
    [flush],
  );

  const value = useMemo(() => ({ isYoda, cache, request }), [isYoda, cache, request]);

  return <YodaTextContext.Provider value={value}>{children}</YodaTextContext.Provider>;
}
