import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Bookmark, Building2, LineChart, LogIn, Wallet } from 'lucide-react';
import { useAuth } from '../auth/useAuth';
import ThemeToggle from './ThemeToggle';
import AuthModal from './AuthModal';
import T from './T';

const TICKERS = [
  { s: 'AAPL', p: '227.52', c: '+1.24%', up: true },
  { s: 'MSFT', p: '481.10', c: '+0.86%', up: true },
  { s: 'TSLA', p: '244.40', c: '-2.13%', up: false },
  { s: 'NVDA', p: '174.05', c: '+3.02%', up: true },
  { s: 'AMZN', p: '231.88', c: '-0.42%', up: false },
  { s: 'GOOGL', p: '201.42', c: '+1.10%', up: true },
];

const FEATURES = [
  { icon: LineChart, title: 'Live charts', body: 'Intraday, weekly and monthly views as candles or lines, drawn from real market data.' },
  { icon: Bookmark, title: 'Personal watchlists', body: 'Curate multiple named lists and track each stock from the day you started watching it.' },
  { icon: Building2, title: 'Company dossiers', body: 'Key fundamentals at a glance — market cap, P/E, dividend yield and the 52-week range.' },
];

/** Public landing page: the entry point into the app and the login flow. */
export default function Landing() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [authOpen, setAuthOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background text-on-surface font-body flex flex-col">
      <header className="border-b border-outline-variant/20">
        <div className="max-w-[1100px] mx-auto px-6 py-5 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Wallet className="text-primary w-7 h-7" />
            <span className="font-headline text-xl font-bold tracking-tight"><T>Architectural Ledger</T></span>
          </div>
          <div className="flex items-center gap-3">
            <ThemeToggle />
            {user ? (
              <button
                type="button"
                onClick={() => navigate('/app')}
                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-on-primary font-bold text-xs uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all"
              >
                <T>Open app</T>
                <ArrowRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setAuthOpen(true)}
                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-on-primary font-bold text-xs uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all"
              >
                <LogIn className="w-4 h-4" />
                <T>Log in</T>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* ticker strip */}
      <div className="border-b border-outline-variant/20 bg-surface/40 overflow-x-auto">
        <div className="flex gap-8 px-6 py-3 whitespace-nowrap text-sm">
          {TICKERS.map((t) => (
            <span key={t.s} className="flex items-center gap-2">
              <span className="font-headline font-bold">{t.s}</span>
              <span className="text-on-surface-variant">{t.p}</span>
              <span className={t.up ? 'text-primary' : 'text-error'}>{t.c}</span>
            </span>
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
          <div className="relative z-10 flex flex-wrap items-center justify-center gap-4">
            <button
              type="button"
              onClick={() => navigate('/app')}
              className="flex items-center gap-2 px-6 py-3.5 rounded-xl bg-primary text-on-primary font-bold text-sm uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all"
            >
              <T>Explore the markets</T>
              <ArrowRight className="w-4 h-4" />
            </button>
            {user ? (
              <button
                type="button"
                onClick={() => navigate('/watchlist')}
                className="flex items-center gap-2 px-6 py-3.5 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm uppercase tracking-wider border border-outline-variant/30 hover:text-primary transition-colors"
              >
                <Bookmark className="w-4 h-4" />
                <T>My watchlists</T>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setAuthOpen(true)}
                className="flex items-center gap-2 px-6 py-3.5 rounded-xl bg-surface-container-high text-on-surface font-bold text-sm uppercase tracking-wider border border-outline-variant/30 hover:text-primary transition-colors"
              >
                <LogIn className="w-4 h-4" />
                <T>Log in</T>
              </button>
            )}
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
