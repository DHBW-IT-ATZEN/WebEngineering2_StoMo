import { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  BarChart3,
  CandlestickChart,
  Eye,
  Info,
  Landmark,
  LineChart,
  Loader2,
  RefreshCw,
  Settings,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import { useMarketData } from '../hooks/useMarketData';
import {
  formatMarketCap,
  formatNumber,
  formatPercent,
  formatPrice,
  formatVolume,
  formatYield,
} from '../utils/format';
import ThemeToggle from './ThemeToggle';
import SymbolSearch from './SymbolSearch';
import PriceChart from './PriceChart';
import T from './T';
import { latestDayVolume, resampleSeries } from '../utils/resample';
import { LIVE_APIS } from '../config';

const CHART_TYPE_KEY = 'stomo-chart-type';
const NAV_ITEMS = ['Market', 'Portfolio', 'Watchlist', 'Settings'];
const TIMEFRAMES = [
  { label: '1D', value: 'INTRADAY' },
  { label: '1W', value: 'WEEKLY' },
  { label: '1M', value: 'MONTHLY' },
];

export default function Dashboard() {
  const [symbol, setSymbol] = useState('AAPL');
  const [timeframe, setTimeframe] = useState('WEEKLY');
  const [chartType, setChartType] = useState(() => {
    const stored = window.localStorage.getItem(CHART_TYPE_KEY);
    return stored === 'line' || stored === 'candles' ? stored : 'candles';
  });
  const { quote, series, overview, loading, error, reload } = useMarketData(symbol);

  useEffect(() => {
    window.localStorage.setItem(CHART_TYPE_KEY, chartType);
  }, [chartType]);

  // Resample the one cached 30-min series locally — switching timeframe makes no request.
  const bars = useMemo(() => resampleSeries(series, timeframe), [series, timeframe]);
  const latestVolume = useMemo(() => latestDayVolume(series), [series]);

  // Price + change% derived from the Yahoo series so they're available with or without Finnhub:
  //  - latestPrice = close of the most recent fine (10-min) bar
  //  - intervalOpen = open of the first candle of the currently-selected view (so the % matches the chart)
  const latestPrice = useMemo(() => {
    const fine = series?.fine;
    if (fine && fine.length > 0) return fine[fine.length - 1].close;
    const coarse = series?.coarse;
    if (coarse && coarse.length > 0) return coarse[coarse.length - 1].close;
    return null;
  }, [series]);
  const intervalOpen = bars.length > 0 ? bars[0].open ?? bars[0].close : null;
  const pctChange =
    latestPrice != null && intervalOpen != null && intervalOpen !== 0
      ? ((latestPrice - intervalOpen) / intervalOpen) * 100
      : null;

  const up = (pctChange ?? 0) >= 0;
  const trendColor = up ? 'text-primary' : 'text-error';
  const name = overview?.name || quote?.symbol || symbol;
  const exchange = overview?.exchange;

  const low52 = overview?.week52Low;
  const high52 = overview?.week52High;
  const rangePos =
    low52 != null && high52 != null && high52 > low52 && latestPrice != null
      ? Math.min(Math.max(((latestPrice - low52) / (high52 - low52)) * 100, 0), 100)
      : null;

  const tags = [overview?.sector, exchange].filter(Boolean);

  return (
    <div className="min-h-screen bg-background text-on-surface font-body">
      {/* HEADER */}
      <header className="bg-surface/90 backdrop-blur-md sticky top-0 z-50 border-b border-outline-variant/30">
        <div className="max-w-[1920px] mx-auto px-4 sm:px-8 py-5 flex justify-between items-center gap-4">
          <div className="flex items-center gap-4">
            <Wallet className="text-primary w-7 h-7" />
            <h1 className="font-headline text-xl sm:text-2xl font-bold tracking-tight">
              <T>Architectural Ledger</T>
            </h1>
          </div>

          <nav className="hidden lg:flex items-center gap-10">
            {NAV_ITEMS.map((item, i) => (
              <a
                key={item}
                href="#"
                className={
                  i === 0
                    ? 'text-primary border-b-2 border-primary pb-1 transition-all active:scale-95'
                    : 'text-on-surface-variant font-medium hover:text-primary px-3 py-1 rounded transition-colors duration-300'
                }
              >
                <T>{item}</T>
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-3 sm:gap-4">
            <SymbolSearch onSelect={(value) => setSymbol(value)} />
            <ThemeToggle />
          </div>
        </div>
      </header>

      {/* CONTENT */}
      {error ? (
        <ErrorState symbol={symbol} message={error} onRetry={reload} />
      ) : loading && !quote ? (
        <LoadingState symbol={symbol} />
      ) : (
        <main className="max-w-[1920px] mx-auto px-4 sm:px-8 py-10 grid grid-cols-1 lg:grid-cols-12 gap-10 mb-24 lg:mb-10">
          {/* LEFT: chart + stats */}
          <section className="lg:col-span-8 flex flex-col gap-8">
            <div className="flex flex-col gap-2">
              <div className="flex items-baseline gap-4 flex-wrap">
                <h2 className="text-4xl sm:text-5xl font-extrabold font-headline tracking-tighter">
                  {quote?.symbol ?? symbol}
                </h2>
                <span className={`font-headline font-semibold text-base ${trendColor}`}>
                  {formatPrice(latestPrice)}
                </span>
                <span className={`font-headline font-bold text-xl ${trendColor}`}>
                  {formatPercent(pctChange)}
                </span>
                {!LIVE_APIS && (
                  <span
                    title="Finnhub + Alpha Vantage calls are disabled in src/config.js. Yahoo (chart) still works."
                    className="px-2 py-0.5 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant bg-surface-container-high rounded-full"
                  >
                    API saving
                  </span>
                )}
              </div>
              <p className="text-on-surface-variant font-medium tracking-wide uppercase text-xs sm:text-sm">
                {name}
                {exchange ? ` • ${exchange}` : ''}
              </p>
            </div>

            {/* CHART */}
            <div className="bg-surface-container-lowest rounded-xl aspect-[16/9] relative overflow-hidden shadow-2xl border border-outline-variant/10 group">
              <div className="absolute top-6 left-6 flex items-center gap-4 z-10">
                <span className="px-3 py-1 bg-primary text-on-primary text-[10px] font-bold rounded-sm shadow-lg shadow-primary/20">
                  <T>LIVE</T>
                </span>
                <div className="flex bg-surface-container/60 backdrop-blur-md rounded-lg p-1 border border-outline-variant/30">
                  {TIMEFRAMES.map((tf) => (
                    <button
                      key={tf.value}
                      type="button"
                      onClick={() => setTimeframe(tf.value)}
                      className={`px-3 py-1 text-[10px] font-bold rounded transition-colors ${
                        timeframe === tf.value
                          ? 'bg-primary text-on-primary'
                          : 'text-on-surface-variant hover:text-primary hover:bg-primary/10'
                      }`}
                    >
                      {tf.label}
                    </button>
                  ))}
                </div>
              </div>

              <PriceChart bars={bars} type={chartType} view={timeframe} />

              {loading && (
                <div className="absolute top-6 right-6 z-10">
                  <Loader2 className="w-5 h-5 text-primary animate-spin" />
                </div>
              )}

              <div className="absolute bottom-8 right-8 z-10">
                <div className="flex bg-surface-container/80 backdrop-blur-xl rounded-xl p-1 border border-outline-variant/30">
                  <button
                    type="button"
                    onClick={() => setChartType('candles')}
                    aria-pressed={chartType === 'candles'}
                    title="Candlestick chart"
                    className={`p-2 rounded-lg transition-colors ${
                      chartType === 'candles' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-primary'
                    }`}
                  >
                    <CandlestickChart className="w-5 h-5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setChartType('line')}
                    aria-pressed={chartType === 'line'}
                    title="Line chart"
                    className={`p-2 rounded-lg transition-colors ${
                      chartType === 'line' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-primary'
                    }`}
                  >
                    <LineChart className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>

            {/* QUICK STATS */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              <StatCard label="Open" value={formatPrice(quote?.open)} highlighted />
              <StatCard label="High" value={formatPrice(quote?.high)} />
              <StatCard label="Low" value={formatPrice(quote?.low)} />
              <StatCard label="Vol" value={formatVolume(latestVolume)} />
            </div>
          </section>

          {/* RIGHT: intelligence + dossier */}
          <section className="lg:col-span-4 flex flex-col gap-10">
            <div className="flex flex-col gap-6">
              <div className="flex items-center justify-between">
                <h3 className="font-headline text-xl font-bold tracking-tight">
                  <T>Executive Intelligence</T>
                </h3>
                <TrendingUp className="text-primary w-5 h-5" />
              </div>

              <div className="bg-surface-container rounded-2xl p-8 flex flex-col gap-8 shadow-2xl relative overflow-hidden border border-outline-variant/10">
                <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 blur-3xl" />

                <div className="flex justify-between items-center">
                  <div>
                    <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-[0.15em] mb-1">
                      <T>Market Cap</T>
                    </p>
                    <p className="text-2xl font-headline font-extrabold">{formatMarketCap(overview?.marketCap)}</p>
                  </div>
                  <div className="w-12 h-12 bg-surface-container-high rounded-xl flex items-center justify-center border border-outline-variant/20">
                    <BarChart3 className="text-primary" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-8">
                  <div>
                    <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-[0.15em] mb-1">
                      <T>P/E Ratio</T>
                    </p>
                    <p className="text-xl font-headline font-bold">{formatNumber(overview?.peRatio)}</p>
                  </div>
                  <div>
                    <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-[0.15em] mb-1">
                      <T>Div Yield</T>
                    </p>
                    <p className="text-xl font-headline font-bold">{formatYield(overview?.dividendYield)}</p>
                  </div>
                </div>

                <div className="flex flex-col gap-3">
                  <div className="flex justify-between items-end">
                    <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-[0.15em]">
                      <T>52W Range</T>
                    </p>
                    <p className="text-[10px] font-bold">
                      {formatPrice(low52)} — {formatPrice(high52)}
                    </p>
                  </div>
                  <div className="h-2 bg-surface-container-high rounded-full overflow-hidden border border-outline-variant/10">
                    <div
                      className="h-full bg-primary rounded-full shadow-[0_0_10px_rgb(var(--primary)/0.4)] transition-all"
                      style={{ width: `${rangePos ?? 0}%` }}
                    />
                  </div>
                </div>

              </div>
            </div>

            {/* DOSSIER */}
            <div className="flex flex-col gap-6">
              <div className="flex items-center justify-between border-b border-outline-variant/20 pb-2">
                <h3 className="font-headline text-xl font-bold tracking-tight">
                  <T>Corporate Dossier</T>
                </h3>
                <Info className="text-primary w-5 h-5" />
              </div>
              <p className="text-sm text-on-surface-variant leading-relaxed">
                <T>{overview?.description || 'No company profile available for this symbol.'}</T>
              </p>
              {tags.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {tags.map((tag) => (
                    <span
                      key={tag}
                      className="px-3 py-1.5 bg-surface-container-low rounded-full text-[10px] font-bold uppercase tracking-wider text-primary border border-primary/20"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </section>
        </main>
      )}

      {/* MOBILE NAV */}
      <nav className="lg:hidden fixed bottom-0 left-0 w-full z-50 h-20 bg-surface/80 backdrop-blur-xl border-t border-outline-variant/20 flex justify-around items-center px-4">
        <MobileNavItem icon={<BarChart3 />} label="Market" active />
        <MobileNavItem icon={<Landmark />} label="Portfolio" />
        <MobileNavItem icon={<Eye />} label="Watchlist" />
        <MobileNavItem icon={<Settings />} label="Settings" />
      </nav>
    </div>
  );
}

function StatCard({ label, value, highlighted = false }) {
  return (
    <div
      className={`bg-surface-container-low p-6 rounded-xl border-l-4 shadow-sm transition-all hover:border-primary ${
        highlighted ? 'border-primary/30' : 'border-transparent'
      }`}
    >
      <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-[0.1em] mb-1">
        <T>{label}</T>
      </p>
      <p className="text-xl font-headline font-bold">{value}</p>
    </div>
  );
}

function MobileNavItem({ icon, label, active = false }) {
  return (
    <button
      type="button"
      className={`flex flex-col items-center gap-1 transition-all active:scale-90 ${
        active ? 'text-primary' : 'text-on-surface-variant'
      }`}
    >
      {icon}
      <span className="text-[10px] font-bold uppercase tracking-tighter">
        <T>{label}</T>
      </span>
    </button>
  );
}

function LoadingState({ symbol }) {
  return (
    <div className="max-w-[1920px] mx-auto px-8 py-32 flex flex-col items-center justify-center gap-4 text-on-surface-variant">
      <Loader2 className="w-8 h-8 text-primary animate-spin" />
      <p className="text-sm uppercase tracking-widest font-bold">Loading {symbol}…</p>
    </div>
  );
}

function ErrorState({ symbol, message, onRetry }) {
  return (
    <div className="max-w-[1920px] mx-auto px-8 py-32 flex flex-col items-center justify-center gap-5 text-center">
      <div className="w-14 h-14 rounded-2xl bg-error/10 flex items-center justify-center">
        <AlertTriangle className="w-7 h-7 text-error" />
      </div>
      <div className="flex flex-col gap-1">
        <p className="font-headline font-bold text-lg">Could not load {symbol}</p>
        <p className="text-sm text-on-surface-variant max-w-md">{message}</p>
      </div>
      <button
        type="button"
        onClick={onRetry}
        className="flex items-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-[0.2em] hover:brightness-110 active:scale-95 transition-all"
      >
        <RefreshCw className="w-4 h-4" />
        <T>Retry</T>
      </button>
    </div>
  );
}
