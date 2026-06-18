import { useCallback, useEffect, useState } from 'react';
import { ThemeContext } from './themeContext';

const STORAGE_KEY = 'stomo-theme';

function getInitialTheme() {
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === 'yoda' || stored === 'dark' ? stored : 'dark';
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme);

  useEffect(() => {
    document.documentElement.classList.toggle('yoda', theme === 'yoda');
    window.localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((current) => (current === 'dark' ? 'yoda' : 'dark'));
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, isYoda: theme === 'yoda', toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}
