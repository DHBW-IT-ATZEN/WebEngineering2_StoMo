import { useEffect } from 'react';
import { useTranslation } from '../i18n/useTranslation';

/**
 * Renders its (string) children translated for the active target — German (static dictionary,
 * with dynamic machine translation as a fallback), the Yoda theme (yodish), or English
 * (passthrough). request() decides whether an async fetch is needed; it no-ops otherwise.
 */
export default function T({ children }) {
  const { request, resolve } = useTranslation();
  const text = typeof children === 'string' ? children : '';

  useEffect(() => {
    if (text) request(text);
  }, [text, request]);

  if (!text) return children;
  return resolve(text);
}
