import { useCallback, useContext } from 'react';
import { TranslationContext } from './translationContext';

export function useTranslation() {
  const ctx = useContext(TranslationContext);
  if (!ctx) {
    throw new Error('useTranslation must be used within a TranslationProvider');
  }
  return ctx;
}

/**
 * Returns a `t(text)` function for translating attribute strings that can't be wrapped in <T>
 * (placeholders, title, aria-label). German resolves from the static dictionary with a dynamic
 * fallback, Yoda from the async cache; request() enqueues a fetch when needed and no-ops otherwise.
 */
export function useTranslate() {
  const { request, resolve } = useTranslation();
  return useCallback(
    (text) => {
      if (text) request(text);
      return resolve(text);
    },
    [request, resolve],
  );
}
