import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bookmark, Building2, CandlestickChart, LineChart, LogIn, LogOut } from 'lucide-react';
import { useAuth } from '../auth/useAuth';
import { getMovers } from '../api/marketData';
import { displaySymbol, formatPercent } from '../utils/format';
import { useCurrency } from '../currency/useCurrency';
import ThemeToggle from './ThemeToggle';
import LanguageSelect from './LanguageSelect';
import AuthModal from './AuthModal';
import SymbolSearch from './SymbolSearch';
import T from './T';

// Trending symbols offered as one-click shortcuts beside the search (same set as MarketEntry).
const QUICK_PICKS = ['AAPL', 'MSFT', 'NVDA', 'TSLA', 'AMZN', 'GOOGL', 'META'];

// Shown until /api/market/movers responds (and if it's unreachable) so the strip is never empty.
const FALLBACK_TICKERS = [
  { symbol: 'AAPL', price: 227.52, changePct: 1.24 },
  { symbol: 'MSFT', price: 481.10, changePct: 0.86 },
  { symbol: 'NVDA', price: 174.05, changePct: 3.02 },
  { symbol: 'TSLA', price: 244.40, changePct: -2.13 },
  { symbol: 'AMZN', price: 231.88, changePct: -0.42 },
  { symbol: 'GOOGL', price: 201.42, changePct: 1.10 },
];

const FEATURES = [
  { icon: LineChart, title: 'Live charts', body: 'Intraday, weekly and monthly views as candles or lines, drawn from real market data.' },
  { icon: Bookmark, title: 'Personal watchlists', body: 'Curate multiple named lists and track each stock from the day you started watching it.' },
  { icon: Building2, title: 'Company dossiers', body: 'Key fundamentals at a glance — market cap, P/E, dividend yield and the 52-week range.' },
];

/** Public landing page: the entry point into the app and the login flow. */
export default function Landing() {
  const { user, logout } = useAuth();
  const { formatMoney } = useCurrency();
  const navigate = useNavigate();
  const [authOpen, setAuthOpen] = useState(false);
  const [tickers, setTickers] = useState(FALLBACK_TICKERS);

  useEffect(() => {
    let active = true;
    getMovers()
      .then((data) => {
        if (active && Array.isArray(data) && data.length > 0) setTickers(data);
      })
      .catch(() => { /* keep the fallback strip */ });
    return () => { active = false; };
  }, []);

  return (
    <div className="min-h-screen bg-background text-on-surface font-body flex flex-col">
      <style>{`
        @keyframes stomo-marquee { from { transform: translateX(0); } to { transform: translateX(-50%); } }
        .stomo-marquee { animation: stomo-marquee 75s linear infinite; }
        .stomo-marquee:hover { animation-play-state: paused; }
      `}</style>

      <header className="border-b border-outline-variant/20">
        <div className="max-w-[1100px] mx-auto px-6 py-5 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <CandlestickChart className="text-primary w-7 h-7" />
            <span className="font-headline text-xl font-bold tracking-tight"><T>Stock Monitor</T></span>
          </div>
          <div className="flex items-center gap-3">
            <LanguageSelect />
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
                onClick={() => setAuthOpen(true)}
                className="flex items-center gap-1.5 px-3 sm:px-4 py-2 rounded-lg bg-primary text-on-primary font-bold text-xs uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all"
              >
                <LogIn className="w-4 h-4" />
                <span className="hidden sm:inline"><T>Log in</T></span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Live ticker marquee (top-watched symbols, or a fallback set). */}
      <div className="border-b border-outline-variant/20 bg-surface/40 overflow-hidden">
        <div className="flex w-max gap-8 px-6 py-3 stomo-marquee">
          {[...tickers, ...tickers].map((t, i) => (
            <button
              type="button"
              key={`${t.symbol}-${i}`}
              onClick={() => navigate(`/app/${encodeURIComponent(t.symbol)}`)}
              title={`View ${displaySymbol(t.symbol)}`}
              className="group flex items-center gap-2 text-sm whitespace-nowrap"
            >
              <span className="font-headline font-bold group-hover:text-primary transition-colors">{displaySymbol(t.symbol)}</span>
              <span className="text-on-surface-variant">{formatMoney(t.price, t.currency, t.type)}</span>
              <span className={(t.changePct ?? 0) >= 0 ? 'text-primary' : 'text-error'}>
                {formatPercent(t.changePct)}
              </span>
            </button>
          ))}
        </div>
      </div>

      <main className="flex-1 flex flex-col">
        <section className="relative max-w-[1100px] w-full mx-auto px-6 py-20 sm:py-28 flex flex-col items-center text-center gap-8 overflow-hidden">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[36rem] h-[36rem] bg-primary/5 rounded-full blur-3xl -z-0" />
          <span className="relative z-10 px-4 py-1.5 rounded-full bg-surface-container-high border border-primary/20 text-[10px] font-bold uppercase tracking-[0.2em] text-primary">
            <T>Real-time market intelligence</T>
          </span>
          <h1 className="relative z-10 font-headline text-4xl sm:text-6xl font-extrabold tracking-tighter max-w-3xl">
            <T>Track the markets with an architect’s precision</T>
          </h1>
          <p className="relative z-10 text-on-surface-variant max-w-xl text-base sm:text-lg leading-relaxed">
            <T>Research any stock, follow live price action, and build watchlists that measure performance from the moment you start watching.</T>
          </p>
          <div className="relative z-20 w-full max-w-md mx-auto">
            <SymbolSearch fullWidth onSelect={(value) => navigate(`/app/${encodeURIComponent(value)}`)} />
          </div>

          <div className="relative z-10 flex flex-wrap items-center justify-center gap-2">
            <span className="text-[10px] uppercase font-bold tracking-[0.15em] text-on-surface-variant mr-1">
              <T>Popular</T>
            </span>
            {QUICK_PICKS.map((symbol) => (
              <button
                key={symbol}
                type="button"
                onClick={() => navigate(`/app/${encodeURIComponent(symbol)}`)}
                className="px-3 py-1.5 bg-surface-container-low rounded-full text-xs font-bold border border-outline-variant/20 hover:border-primary/40 hover:text-primary transition-colors"
              >
                {symbol}
              </button>
            ))}
          </div>
        </section>

        <section className="max-w-[1100px] w-full mx-auto px-6 pb-24 grid grid-cols-1 md:grid-cols-3 gap-6">
          {FEATURES.map((feature) => {
            const Icon = feature.icon;
            return (
              <div
                key={feature.title}
                className="bg-surface-container rounded-2xl p-8 flex flex-col gap-4 border border-outline-variant/10 shadow-lg"
              >
                <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                  <Icon className="text-primary w-6 h-6" />
                </div>
                <h3 className="font-headline text-lg font-bold"><T>{feature.title}</T></h3>
                <p className="text-sm text-on-surface-variant leading-relaxed"><T>{feature.body}</T></p>
              </div>
            );
          })}
        </section>
      </main>

      <footer className="border-t border-outline-variant/20">
        <div className="max-w-[1100px] mx-auto px-6 py-6 text-xs text-on-surface-variant flex items-center justify-between">
          <span>StoMo · Stock Monitor</span>
          <span><T>Web Engineering 2</T></span>
        </div>
      </footer>

      {authOpen && (
        <AuthModal
          onClose={() => setAuthOpen(false)}
          onSuccess={() => { setAuthOpen(false); navigate('/watchlist'); }}
        />
      )}
    </div>
  );
}
