import { useContext } from 'react';
import { YodaTextContext } from './yodaTextContext';

export function useYodaText() {
  const ctx = useContext(YodaTextContext);
  if (!ctx) {
    throw new Error('useYodaText must be used within a YodaTextProvider');
  }
  return ctx;
}
