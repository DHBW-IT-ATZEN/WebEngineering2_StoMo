import { useEffect } from 'react';
import { useTranslation } from '../i18n/useTranslation';

/**
 * Renders its (string) children translated for the active target — German (static dictionary),
 * the Yoda theme (yodish), or English (passthrough). Only Yoda needs an async request.
 */
export default function T({ children }) {
  const { target, request, resolve } = useTranslation();
  const text = typeof children === 'string' ? children : '';

  useEffect(() => {
    if (text && target === 'yoda') request(text);
  }, [text, target, request]);

  if (!text) return children;
  return resolve(text);
}
