import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { TranslationContext } from './translationContext';
import { useTheme } from '../theme/useTheme';
import { useLanguage } from './useLanguage';
import { de } from './de';
import { translateBatch as translateYodish } from '../api/yoda';

/**
 * Resolves UI strings for the active target:
 *  - 'de'   -> static dictionary (de.js), synchronous, hand-authored wording.
 *  - 'yoda' -> yodish via the backend, batched + cached in memory (it's dynamic, not static).
 *  - 'en'   -> passthrough (the source string).
 * Only the Yoda path makes network calls (request()); German is instant and offline.
 */
export function TranslationProvider({ children }) {
  const { isYoda } = useTheme();
  const { language } = useLanguage();
  const target = isYoda ? 'yoda' : language === 'de' ? 'de' : 'en';

  const [yodaCache, setYodaCache] = useState({});
  const cacheRef = useRef(yodaCache);
  const pending = useRef(new Set());
  const timer = useRef(null);

  useEffect(() => {
    cacheRef.current = yodaCache;
  }, [yodaCache]);

  const flush = useCallback(() => {
    const texts = [...pending.current].filter((s) => !(s in cacheRef.current));
    pending.current.clear();
    if (texts.length === 0) return;
    translateYodish(texts)
      .then((map) => setYodaCache((prev) => ({ ...prev, ...map })))
      .catch(() => {
        /* yodish is best-effort; keep showing the source text */
      });
  }, []);

  // Enqueue a string for yodish translation (no-op for static 'de' / passthrough 'en').
  const request = useCallback(
    (text) => {
      if (!text || text in cacheRef.current) return;
      pending.current.add(text);
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(flush, 80);
    },
    [flush],
  );

  const resolve = useCallback(
    (text) => {
      if (!text) return text;
      if (target === 'de') return de[text] ?? text;
      if (target === 'yoda') return yodaCache[text] ?? text;
      return text; // en
    },
    [target, yodaCache],
  );

  const value = useMemo(() => ({ target, request, resolve }), [target, request, resolve]);

  return <TranslationContext.Provider value={value}>{children}</TranslationContext.Provider>;
}
