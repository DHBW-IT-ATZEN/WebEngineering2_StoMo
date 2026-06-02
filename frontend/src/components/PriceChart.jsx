import { useState } from 'react';
import { formatNumber, formatPrice } from '../utils/format';

const MAX_BARS = 40;

function tickLabel(dateStr, view) {
  if (!dateStr) return '';
  if (view === 'INTRADAY' && dateStr.length >= 16) {
    return dateStr.substring(11, 16); // HH:mm
  }
  const day = dateStr.length >= 10 ? dateStr.substring(0, 10) : dateStr;
  const d = new Date(`${day}T00:00:00Z`);
  if (Number.isNaN(d.getTime())) return day;
  return d.toLocaleString('en-US', { month: 'short', day: '2-digit', timeZone: 'UTC' });
}

function hoverLabel(dateStr, view) {
  if (!dateStr) return '';
  const datePart = tickLabel(dateStr, 'WEEKLY');
  if (view === 'INTRADAY' && dateStr.length >= 16) {
    return `${datePart} · ${dateStr.substring(11, 16)}`;
  }
  return datePart;
}

export default function PriceChart({ bars: input, type = 'candles', view = 'WEEKLY' }) {
  const bars = (input ?? []).slice(-MAX_BARS);
  const [hover, setHover] = useState(null);

  if (bars.length === 0) {
    return (
      <div className="absolute inset-0 flex items-center justify-center text-on-surface-variant text-sm">
        No chart data available
      </div>
    );
  }

  const highs = bars.map((b) => b.high ?? b.close ?? 0);
  const lows = bars.map((b) => b.low ?? b.close ?? 0);
  const max = Math.max(...highs);
  const min = Math.min(...lows);
  const range = max - min || 1;
  const pct = (value) => ((value - min) / range) * 100;

  function handleMove(event) {
    const rect = event.currentTarget.getBoundingClientRect();
    const ratio = (event.clientX - rect.left) / rect.width;
    const idx = Math.max(0, Math.min(bars.length - 1, Math.floor(ratio * bars.length)));
    setHover({ idx, xPct: ((idx + 0.5) / bars.length) * 100 });
  }
  function handleLeave() {
    setHover(null);
  }

  const hoverBar = hover ? bars[hover.idx] : null;

  // Four evenly-spaced ticks. flex-justify-between handles edge alignment
  // (first label flush-left, last flush-right) so they can never clip the chart bounds.
  const n = bars.length;
  const ticks = [
    bars[0],
    bars[Math.floor(n / 3)],
    bars[Math.floor((2 * n) / 3)],
    bars[n - 1],
  ];

  return (
    <div className="absolute inset-0 p-8 pt-24 pb-3 flex flex-col">
      {/* Hover readout pinned to the top-right of the chart area */}
      {hoverBar && (
        <div className="absolute top-6 right-6 z-20 bg-surface-container/90 backdrop-blur-md px-3 py-2 rounded-lg border border-outline-variant/30 pointer-events-none text-right shadow-lg">
          <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
            {hoverLabel(hoverBar.date, view)}
          </p>
          <p className="text-base font-headline font-bold text-on-surface">
            {formatPrice(hoverBar.close)}
          </p>
          <div className="flex justify-end gap-3 mt-1 text-[10px] font-medium text-on-surface-variant tabular-nums">
            <span>
              <span className="text-on-surface-variant/60 mr-0.5">O</span>
              {formatNumber(hoverBar.open, 2)}
            </span>
            <span>
              <span className="text-on-surface-variant/60 mr-0.5">H</span>
              {formatNumber(hoverBar.high, 2)}
            </span>
            <span>
              <span className="text-on-surface-variant/60 mr-0.5">L</span>
              {formatNumber(hoverBar.low, 2)}
            </span>
          </div>
        </div>
      )}

      {/* Bars / line live here. flex-1 gives the rest of the vertical space, leaving room for ticks below. */}
      <div
        className="relative flex-1 cursor-crosshair"
        onMouseMove={handleMove}
        onMouseLeave={handleLeave}
      >
        {/* Ambient grid lines, sized to the bars area only */}
        <div className="absolute inset-0 flex flex-col justify-between pointer-events-none opacity-10">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="border-t border-on-surface w-full" />
          ))}
        </div>

        {type === 'line' ? <LineView bars={bars} pct={pct} /> : <CandleView bars={bars} pct={pct} />}

        {/* Vertical crosshair */}
        {hover && (
          <div
            className="absolute top-0 bottom-0 w-px bg-on-surface/40 pointer-events-none"
            style={{ left: `${hover.xPct}%` }}
          />
        )}
      </div>

      {/* X-axis ticks — first/last anchor to the edges, middle pair sits in flow */}
      <div className="mt-2 flex justify-between text-[10px] font-medium uppercase tracking-wider text-on-surface-variant pointer-events-none">
        {ticks.map((bar, i) => (
          <span key={`${i}-${bar?.date ?? i}`}>{tickLabel(bar?.date, view)}</span>
        ))}
      </div>
    </div>
  );
}

function CandleView({ bars, pct }) {
  return (
    <div className="absolute inset-0 flex items-end gap-1">
      {bars.map((bar, i) => {
        const open = bar.open ?? bar.close ?? 0;
        const close = bar.close ?? bar.open ?? 0;
        const up = close >= open;
        const bodyTop = pct(Math.max(open, close));
        const bodyBottom = pct(Math.min(open, close));
        const wickTop = pct(bar.high ?? Math.max(open, close));
        const wickBottom = pct(bar.low ?? Math.min(open, close));
        const bodyColor = up
          ? 'bg-primary/80 group-hover/bar:bg-primary'
          : 'bg-tertiary/80 group-hover/bar:bg-tertiary';
        const wickColor = up ? 'bg-primary/30' : 'bg-tertiary/30';

        return (
          <div key={bar.date ?? i} className="relative flex-1 h-full group/bar">
            <div
              className={`absolute left-1/2 -translate-x-1/2 w-px ${wickColor}`}
              style={{ bottom: `${wickBottom}%`, height: `${Math.max(wickTop - wickBottom, 0.5)}%` }}
            />
            <div
              className={`absolute left-1/2 -translate-x-1/2 w-[55%] max-w-[20px] rounded-sm transition-colors ${bodyColor}`}
              style={{ bottom: `${bodyBottom}%`, height: `${Math.max(bodyTop - bodyBottom, 1.5)}%` }}
            />
          </div>
        );
      })}
    </div>
  );
}

function LineView({ bars, pct }) {
  const n = bars.length;
  const points = bars.map((bar, i) => {
    const x = n === 1 ? 50 : (i / (n - 1)) * 100;
    const y = 100 - pct(bar.close ?? bar.open ?? 0);
    return `${x.toFixed(2)},${y.toFixed(2)}`;
  });
  const line = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p}`).join(' ');
  const area = `${line} L100,100 L0,100 Z`;

  return (
    <svg className="absolute inset-0 w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
      <defs>
        <linearGradient id="priceLineFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="rgb(var(--primary))" stopOpacity="0.25" />
          <stop offset="100%" stopColor="rgb(var(--primary))" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill="url(#priceLineFill)" />
      <path
        d={line}
        fill="none"
        stroke="rgb(var(--primary))"
        strokeWidth="1.5"
        vectorEffect="non-scaling-stroke"
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  );
}
