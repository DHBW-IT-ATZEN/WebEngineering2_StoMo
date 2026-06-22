import React, { useState } from 'react';
import { 
  CandlestickChart, Diamond, Zap, TrendingUp,
  Search, Eye, Settings, BarChart3, 
  Info, Landmark, ZoomIn, Brush 
} from 'lucide-react';

const Dashboard = () => {
  const [zenMode, setZenMode] = useState(false);

  return (
    <div className={`min-h-screen ${zenMode ? 'yoda-theme' : ''} bg-background text-on-surface font-body`}>
      
      {/* HEADER */}
      <header className="bg-surface/90 backdrop-blur-md sticky top-0 z-50 border-b border-outline-variant/10">
        <div className="max-w-[1920px] mx-auto px-8 py-6 flex justify-between items-center">
          <div className="flex items-center gap-4">
            <CandlestickChart className="text-primary w-8 h-8" />
            <h1 className="font-headline text-2xl font-bold tracking-tight">Stock Monitor</h1>
          </div>
          
          <nav className="hidden md:flex items-center gap-8">
            {['Market', 'Portfolio', 'Watchlist', 'Settings'].map((item, i) => (
              <a key={item} href="#" className={`${i === 0 ? 'text-primary border-b-2 border-primary' : 'text-on-surface-variant'} hover:text-primary transition-all pb-1 font-medium`}>
                {item}
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-6">
            <div 
              onClick={() => setZenMode(!zenMode)}
              className="flex items-center gap-3 bg-surface-container-high px-4 py-2 rounded-full cursor-pointer hover:bg-surface-container-highest transition-all"
            >
              <span className="text-[10px] font-bold uppercase tracking-widest text-primary">Zen Mode</span>
              <div className={`w-10 h-5 rounded-full relative p-1 flex items-center border border-primary/30 ${zenMode ? 'bg-primary/40 justify-end' : 'bg-primary/10 justify-start'}`}>
                <div className="w-3 h-3 bg-primary rounded-full shadow-sm" />
              </div>
              <Diamond className={`w-4 h-4 text-primary ${zenMode ? 'fill-primary' : ''}`} />
            </div>
          </div>
        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className="max-w-[1920px] mx-auto px-8 py-10 grid grid-cols-1 lg:grid-cols-12 gap-10">
        
        {/* LEFT COLUMN: Market Data & Chart */}
        <section className="lg:col-span-8 space-y-8">
          <div className="space-y-1">
            <div className="flex items-baseline gap-4">
              <h2 className="text-5xl font-extrabold font-headline tracking-tighter">AAPL.US</h2>
              <span className="text-primary font-headline font-bold text-xl">+2.45%</span>
            </div>
            <p className="text-on-surface-variant font-medium tracking-wide uppercase text-xs">
              Apple Inc. • Nasdaq Global Select Market
            </p>
          </div>

          {/* CHART AREA */}
          <div className="bg-surface-container-lowest rounded-xl aspect-video relative overflow-hidden shadow-2xl border border-outline-variant/10 group">
            <div className="absolute top-6 left-6 flex gap-4 z-10">
              <span className="px-3 py-1 bg-primary text-on-primary text-[10px] font-bold rounded-sm shadow-lg shadow-primary/20">LIVE</span>
              <div className="flex bg-surface-container/60 backdrop-blur-md rounded-lg p-1 border border-outline-variant/30">
                {['1H', '4H', '1D', '1W', '1M'].map((t) => (
                  <button key={t} className={`px-3 py-1 text-[10px] font-bold rounded ${t === '1H' ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-primary'}`}>
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* SIMULATED CHART BARS */}
            <div className="w-full h-full p-8 pt-24 flex items-end justify-between gap-2">
              {[40, 60, 45, 30, 70, 55, 35, 65, 80].map((h, i) => (
                <div key={i} className="flex-1 flex flex-col items-center gap-1 h-full justify-end group/bar">
                  <div className={`w-full max-w-[28px] rounded-sm transition-all duration-500 shadow-lg ${i % 3 === 0 ? 'bg-tertiary h-['+h+'%] shadow-tertiary/20' : 'bg-primary h-['+h+'%] shadow-primary/20'}`} 
                       style={{ height: `${h}%` }} />
                </div>
              ))}
            </div>

            <div className="absolute bottom-8 right-8 flex gap-3">
              <button className="bg-surface-container/80 backdrop-blur-xl p-3 rounded-xl border border-outline-variant/30 hover:border-primary transition-all">
                <ZoomIn className="w-5 h-5 text-primary" />
              </button>
              <button className="bg-surface-container/80 backdrop-blur-xl p-3 rounded-xl border border-outline-variant/30 hover:border-primary transition-all">
                <Brush className="w-5 h-5 text-primary" />
              </button>
            </div>
          </div>

          {/* QUICK STATS */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {[
              { label: 'Open', val: '$189.43' },
              { label: 'High', val: '$192.67' },
              { label: 'Low', val: '$188.12' },
              { label: 'Vol', val: '52.4M' }
            ].map((stat) => (
              <div key={stat.label} className="bg-surface-container-low p-6 rounded-xl border-l-4 border-transparent hover:border-primary transition-all shadow-sm">
                <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-widest mb-1">{stat.label}</p>
                <p className="text-xl font-headline font-bold">{stat.val}</p>
              </div>
            ))}
          </div>
        </section>

        {/* RIGHT COLUMN: Executive Intelligence */}
        <section className="lg:col-span-4 space-y-10">
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="font-headline text-xl font-bold tracking-tight">Executive Intelligence</h3>
              <TrendingUp className="text-primary w-5 h-5" />
            </div>

            <div className="bg-surface-container rounded-2xl p-8 space-y-8 shadow-2xl relative overflow-hidden border border-outline-variant/10">
              <div className="flex justify-between items-center">
                <div>
                  <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-widest mb-1">Market Cap</p>
                  <p className="text-3xl font-headline font-extrabold text-on-surface">$3.02T</p>
                </div>
                <div className="w-12 h-12 bg-surface-container-high rounded-xl flex items-center justify-center border border-outline-variant/20">
                  <BarChart3 className="text-primary" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-8">
                <div>
                  <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-widest mb-1">P/E Ratio</p>
                  <p className="text-xl font-headline font-bold">31.45</p>
                </div>
                <div>
                  <p className="text-on-surface-variant text-[10px] uppercase font-bold tracking-widest mb-1">Div Yield</p>
                  <p className="text-xl font-headline font-bold">0.52%</p>
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest">
                  <span className="text-on-surface-variant">52W Range</span>
                  <span>$124.17 — $198.23</span>
                </div>
                <div className="h-2 bg-surface-container-high rounded-full overflow-hidden border border-outline-variant/10">
                  <div className="h-full bg-primary w-[82%] rounded-full shadow-[0_0_10px_rgba(71,234,237,0.4)]" />
                </div>
              </div>

              <button className="w-full py-4 rounded-xl bg-primary text-on-primary font-bold text-xs uppercase tracking-[0.2em] shadow-lg shadow-primary/20 hover:brightness-110 active:scale-95 transition-all">
                Execute Transaction
              </button>
            </div>
          </div>

          {/* DOSSIER */}
          <div className="space-y-6">
            <div className="flex items-center justify-between border-b border-outline-variant/20 pb-2">
              <h3 className="font-headline text-xl font-bold tracking-tight">Corporate Dossier</h3>
              <Info className="text-primary w-5 h-5" />
            </div>
            <div className="space-y-4 text-sm text-on-surface-variant leading-relaxed">
              <p>Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide.</p>
              <p>As a pillar of the global technology sector, Apple's architectural strategy focuses on ecosystem lock-in and high-margin services.</p>
            </div>
            <div className="flex flex-wrap gap-2">
              {['Technology', 'Electronics', 'Blue Chip'].map(tag => (
                <span key={tag} className="px-3 py-1.5 bg-surface-container-low rounded-full text-[10px] font-bold uppercase tracking-wider text-primary border border-primary/20">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        </section>
      </main>

      {/* MOBILE NAV */}
      <nav className="md:hidden fixed bottom-0 left-0 w-full z-50 h-20 bg-surface/80 backdrop-blur-xl border-t border-outline-variant/20 flex justify-around items-center px-4">
        <NavIcon icon={<BarChart3 />} label="Market" active />
        <NavIcon icon={<Landmark />} label="Portfolio" />
        <NavIcon icon={<Eye />} label="Watchlist" />
        <NavIcon icon={<Settings />} label="Settings" />
      </nav>
    </div>
  );
};

const NavIcon = ({ icon, label, active }) => (
  <button className={`flex flex-col items-center gap-1 ${active ? 'text-primary' : 'text-on-surface-variant'}`}>
    {React.cloneElement(icon, { size: 20, fill: active ? 'currentColor' : 'none' })}
    <span className="text-[10px] font-bold uppercase tracking-tighter">{label}</span>
  </button>
);

export default Dashboard;