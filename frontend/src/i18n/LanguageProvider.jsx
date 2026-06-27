import { useCallback, useEffect, useMemo, useState } from 'react';
import { LanguageContext } from './languageContext';

const STORAGE_KEY = 'stomo-lang';
const LOCALES = { en: 'en-US', de: 'de-DE' };

function getInitialLanguage() {
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === 'de' || stored === 'en' ? stored : 'en';
}

/** The active UI language ('en' | 'de'), persisted, plus the matching Intl locale for formatting. */
export function LanguageProvider({ children }) {
  const [language, setLanguageState] = useState(getInitialLanguage);

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, language);
    document.documentElement.lang = language;
  }, [language]);

  const setLanguage = useCallback((lang) => setLanguageState(lang === 'de' ? 'de' : 'en'), []);

  const value = useMemo(
    () => ({ language, setLanguage, locale: LOCALES[language] }),
    [language, setLanguage],
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}
