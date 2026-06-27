import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { TranslationContext } from './translationContext';
import { useTheme } from '../theme/useTheme';
import { useLanguage } from './useLanguage';
import { de } from './de';
import { translateBatch as translateYodish } from '../api/yoda';
import { translateBatch as translateTo } from '../api/translate';

/**
 * Resolves UI strings for the active target:
 *  - 'de'   -> the static dictionary (de.js) for hand-authored UI labels, falling back to dynamic
 *              machine translation (LibreTranslate, via /api/translate) for anything not in it,
 *              such as company descriptions. Both are cached in memory.
 *  - 'yoda' -> yodish via the backend, batched + cached.
 *  - 'en'   -> passthrough (the source string).
 *
 * Static labels stay instant and offline; only cache-missing dynamic text triggers a network call.
 */
export function TranslationProvider({ children }) {
  const { isYoda } = useTheme();
  const { language } = useLanguage();
  const target = isYoda ? 'yoda' : language === 'de' ? 'de' : 'en';

  // One in-memory cache per dynamic target.
  const [cache, setCache] = useState({ de: {}, yoda: {} });
  const cacheRef = useRef(cache);
  const pending = useRef(new Set());
  const timer = useRef(null);

  useEffect(() => {
    cacheRef.current = cache;
  }, [cache]);

  // Send everything queued for `flushTarget` to its translation backend, then cache the results.
  const flush = useCallback((flushTarget) => {
    const current = cacheRef.current[flushTarget] || {};
    const texts = [...pending.current].filter((s) => !(s in current));
    pending.current.clear();
    if (texts.length === 0) return;
    const request = flushTarget === 'yoda' ? translateYodish(texts) : translateTo(texts, flushTarget);
    request
      .then((map) => setCache((prev) => ({ ...prev, [flushTarget]: { ...prev[flushTarget], ...map } })))
      .catch(() => {
        /* best-effort; keep showing the source text */
      });
  }, []);

  // Enqueue a string for dynamic translation. No-op for English, for German strings the static
  // dictionary already covers, or anything already cached.
  const request = useCallback(
    (text) => {
      if (!text || target === 'en') return;
      if (target === 'de' && de[text] != null) return;
      if (text in (cacheRef.current[target] || {})) return;
      pending.current.add(text);
      if (timer.current) clearTimeout(timer.current);
      const flushTarget = target;
      timer.current = setTimeout(() => flush(flushTarget), 80);
    },
    [target, flush],
  );

  const resolve = useCallback(
    (text) => {
      if (!text) return text;
      if (target === 'de') return de[text] ?? cache.de[text] ?? text;
      if (target === 'yoda') return cache.yoda[text] ?? text;
      return text; // en
    },
    [target, cache],
  );

  const value = useMemo(() => ({ target, request, resolve }), [target, request, resolve]);

  return <TranslationContext.Provider value={value}>{children}</TranslationContext.Provider>;
}
