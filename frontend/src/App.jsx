import { BrowserRouter, Navigate, Route, Routes, useOutletContext } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeProvider';
import { YodaTextProvider } from './theme/YodaTextProvider';
import { AuthProvider } from './auth/AuthProvider';
import Landing from './components/Landing';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import WatchlistPage from './components/WatchlistPage';

/** /app — the stock dashboard, wired to the shared symbol + login handler from the Layout. */
function MarketView() {
  const { symbol, requireLogin } = useOutletContext();
  return <Dashboard symbol={symbol} onRequireLogin={() => requireLogin()} />;
}

function App() {
  return (
    <ThemeProvider>
      <YodaTextProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<Landing />} />
              <Route element={<Layout />}>
                <Route path="/app" element={<MarketView />} />
                <Route path="/watchlist" element={<WatchlistPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </YodaTextProvider>
    </ThemeProvider>
  );
}

export default App;
