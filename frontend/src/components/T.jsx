import { useEffect } from 'react';
import { useYodaText } from '../theme/useYodaText';

/**
 * Renders its (string) children, translated to Yodish while Yoda Mode is active.
 * In dark mode it renders the original text and makes no API calls.
 */
export default function T({ children }) {
  const { isYoda, cache, request } = useYodaText();
  const text = typeof children === 'string' ? children : '';

  useEffect(() => {
    if (isYoda && text) request(text);
  }, [isYoda, text, request]);

  if (!isYoda || !text) return children;
  return cache[text] ?? text;
}
