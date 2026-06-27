import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useOutletContext, useParams } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeProvider';
import { LanguageProvider } from './i18n/LanguageProvider';
import { TranslationProvider } from './i18n/TranslationProvider';
import { AuthProvider } from './auth/AuthProvider';
import { CurrencyProvider } from './currency/CurrencyProvider';
import Landing from './components/Landing';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import MarketEntry from './components/MarketEntry';
import WatchlistPage from './components/WatchlistPage';

/** /app/:symbol — the stock dashboard for the symbol in the URL (shareable/bookmarkable). */
function MarketView() {
  const { symbol } = useParams();
  const { requireLogin } = useOutletContext();
  const upper = (symbol || '').toUpperCase();
  // Remember the last stock viewed so switching tabs (Market <-> Watchlist) resumes it.
  useEffect(() => {
    if (upper) window.localStorage.setItem('stomo-last-symbol', upper);
  }, [upper]);
  return <Dashboard symbol={upper} onRequireLogin={() => requireLogin()} />;
}

function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <TranslationProvider>
          <AuthProvider>
            <CurrencyProvider>
              <BrowserRouter>
                <Routes>
                  <Route path="/" element={<Landing />} />
                  <Route element={<Layout />}>
                    <Route path="/app" element={<MarketEntry />} />
                    <Route path="/app/:symbol" element={<MarketView />} />
                    <Route path="/watchlist" element={<WatchlistPage />} />
                  </Route>
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </BrowserRouter>
            </CurrencyProvider>
          </AuthProvider>
        </TranslationProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
}

export default App;
