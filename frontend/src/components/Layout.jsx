import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { BarChart3, Bookmark, LogIn, LogOut, Wallet } from 'lucide-react';
import SymbolSearch from './SymbolSearch';
import ThemeToggle from './ThemeToggle';
import AuthModal from './AuthModal';
import T from './T';
import { useAuth } from '../auth/useAuth';

/**
 * In-app shell: the header (brand, Market/Watchlist nav, ticker search, theme toggle,
 * login/logout) plus the routed view via <Outlet />. Owns the selected symbol and the auth
 * modal, shared with child routes through the outlet context. Only the watchlist needs login.
 */
export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [symbol, setSymbol] = useState('AAPL');
  const [authOpen, setAuthOpen] = useState(false);
  const [afterAuth, setAfterAuth] = useState(null);

  function requireLogin(redirectTo = null) {
    setAfterAuth(redirectTo);
    setAuthOpen(true);
  }

  function handleAuthSuccess() {
    setAuthOpen(false);
    const dest = afterAuth;
    setAfterAuth(null);
    if (dest) navigate(dest);
  }

  function selectSymbol(value) {
    setSymbol(value);
    navigate('/app');
  }

  function goWatchlist() {
    if (user) navigate('/watchlist');
    else requireLogin('/watchlist');
  }

  const onMarket = pathname.startsWith('/app');
  const onWatchlist = pathname.startsWith('/watchlist');

  return (
    <div className="min-h-screen bg-background text-on-surface font-body">
      <header className="bg-surface/90 backdrop-blur-md sticky top-0 z-50 border-b border-outline-variant/30">
        <div className="max-w-[1920px] mx-auto px-4 sm:px-8 py-5 flex justify-between items-center gap-4">
          <button type="button" onClick={() => navigate('/app')} className="flex items-center gap-4">
            <Wallet className="text-primary w-7 h-7" />
            <h1 className="font-headline text-xl sm:text-2xl font-bold tracking-tight">
              <T>Architectural Ledger</T>
            </h1>
          </button>

          <nav className="hidden lg:flex items-center gap-10">
            <NavTab label="Market" active={onMarket} onClick={() => navigate('/app')} />
            <NavTab label="Watchlist" active={onWatchlist} onClick={goWatchlist} />
          </nav>

          <div className="flex items-center gap-3 sm:gap-4">
            <SymbolSearch onSelect={selectSymbol} />
            <ThemeToggle />
            {user ? (
              <div className="flex items-center gap-2 sm:gap-3">
                <span className="hidden sm:inline text-sm font-medium text-on-surface-variant">
                  {user.firstname}
                </span>
                <button
                  type="button"
                  onClick={logout}
                  title="Log out"
                  className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-on-surface-variant hover:text-primary border border-outline-variant/30 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  <span className="hidden sm:inline text-xs font-bold uppercase tracking-wider"><T>Logout</T></span>
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => requireLogin()}
                className="flex items-center gap-1.5 px-3 sm:px-4 py-2 rounded-lg bg-primary text-on-primary font-bold text-xs uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all"
              >
                <LogIn className="w-4 h-4" />
                <span className="hidden sm:inline"><T>Log in</T></span>
              </button>
            )}
          </div>
        </div>
      </header>

      <Outlet context={{ symbol, selectSymbol, requireLogin }} />

      <nav className="lg:hidden fixed bottom-0 left-0 w-full z-50 h-20 bg-surface/80 backdrop-blur-xl border-t border-outline-variant/20 flex justify-around items-center px-4">
        <MobileNavItem icon={<BarChart3 />} label="Market" active={onMarket} onClick={() => navigate('/app')} />
        <MobileNavItem icon={<Bookmark />} label="Watchlist" active={onWatchlist} onClick={goWatchlist} />
      </nav>

      {authOpen && <AuthModal onClose={() => setAuthOpen(false)} onSuccess={handleAuthSuccess} />}
    </div>
  );
}

function NavTab({ label, active, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        active
          ? 'text-primary border-b-2 border-primary pb-1 transition-all active:scale-95'
          : 'text-on-surface-variant font-medium hover:text-primary px-3 py-1 rounded transition-colors duration-300'
      }
    >
      <T>{label}</T>
    </button>
  );
}

function MobileNavItem({ icon, label, active = false, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex flex-col items-center gap-1 transition-all active:scale-90 ${
        active ? 'text-primary' : 'text-on-surface-variant'
      }`}
    >
      {icon}
      <span className="text-[10px] font-bold uppercase tracking-tighter"><T>{label}</T></span>
    </button>
  );
}
