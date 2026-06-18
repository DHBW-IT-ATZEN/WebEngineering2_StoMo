import { BrowserRouter, Navigate, Route, Routes, useOutletContext, useParams } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeProvider';
import { YodaTextProvider } from './theme/YodaTextProvider';
import { AuthProvider } from './auth/AuthProvider';
import Landing from './components/Landing';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import MarketEntry from './components/MarketEntry';
import WatchlistPage from './components/WatchlistPage';

/** /app/:symbol — the stock dashboard for the symbol in the URL (shareable/bookmarkable). */
function MarketView() {
  const { symbol } = useParams();
  const { requireLogin } = useOutletContext();
  return <Dashboard symbol={(symbol || '').toUpperCase()} onRequireLogin={() => requireLogin()} />;
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
                <Route path="/app" element={<MarketEntry />} />
                <Route path="/app/:symbol" element={<MarketView />} />
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
