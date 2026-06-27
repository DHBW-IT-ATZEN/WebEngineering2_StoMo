import { useOutletContext } from 'react-router-dom';
import { TrendingUp } from 'lucide-react';
import SymbolSearch from './SymbolSearch';
import T from './T';

const QUICK_PICKS = ['AAPL', 'MSFT', 'NVDA', 'TSLA', 'AMZN', 'GOOGL', 'META'];

/** The /app entry screen: a centered search + quick picks instead of an arbitrary default stock. */
export default function MarketEntry() {
  const { selectSymbol } = useOutletContext();

  return (
    <main className="max-w-[820px] mx-auto px-6 py-24 sm:py-32 flex flex-col items-center text-center gap-8 mb-24 lg:mb-10">
      <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center">
        <TrendingUp className="w-7 h-7 text-primary" />
      </div>
      <div className="flex flex-col gap-2">
        <h2 className="font-headline text-3xl sm:text-4xl font-extrabold tracking-tight">
          <T>Research a stock</T>
        </h2>
        <p className="text-on-surface-variant">
          <T>Search any ticker to see its live chart, key stats and company dossier.</T>
        </p>
      </div>

      <div className="w-full max-w-md mx-auto">
        <SymbolSearch fullWidth onSelect={selectSymbol} />
      </div>

      <div className="flex flex-wrap items-center justify-center gap-2">
        <span className="text-[10px] uppercase font-bold tracking-[0.15em] text-on-surface-variant mr-1">
          <T>Popular</T>
        </span>
        {QUICK_PICKS.map((symbol) => (
          <button
            key={symbol}
            type="button"
            onClick={() => selectSymbol(symbol)}
            className="px-3 py-1.5 bg-surface-container-low rounded-full text-xs font-bold border border-outline-variant/20 hover:border-primary/40 hover:text-primary transition-colors"
          >
            {symbol}
          </button>
        ))}
      </div>
    </main>
  );
}
