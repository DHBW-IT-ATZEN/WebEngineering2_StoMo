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
 * (placeholders, title, aria-label). Resolves German statically and Yoda from the async cache
 * (enqueuing a fetch on first use); English passes through.
 */
export function useTranslate() {
  const { target, request, resolve } = useTranslation();
  return useCallback(
    (text) => {
      if (text && target === 'yoda') request(text);
      return resolve(text);
    },
    [target, request, resolve],
  );
}
